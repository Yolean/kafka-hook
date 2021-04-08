package se.yolean.kafka.hook.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import se.yolean.kafka.hook.CloudeventConfiguration;
import se.yolean.kafka.hook.CloudeventExtender;
import se.yolean.kafka.hook.HookCloudEvent;
import se.yolean.kafka.hook.LimitsConfiguration;
import se.yolean.kafka.hook.Producer;
import se.yolean.kafka.hook.cloudevents.IncomingWebhookExtension;
import se.yolean.kafka.hook.types.v1.HookError;
import se.yolean.kafka.hook.types.v1.HookMessageKey;
import se.yolean.kafka.hook.types.v1.HookReceipt;

@ApplicationScoped
public class KafkaHookResource {

  private static final Logger logger = LoggerFactory.getLogger(KafkaHookResource.class);

  @Inject Producer producer;
  @Inject CloudeventConfiguration config;
  @Inject LimitsConfiguration limits;
  @Inject CloudeventExtender extensions;

  private final Map<HookError.Error, Counter> countProduceErrors;

  public KafkaHookResource(MeterRegistry registry) {
    countProduceErrors = new HashMap<>();
    countProduceErrors.put(null, registry.counter("produce.errors", Arrays.asList(Tag.of("error", "undefined"))));
    for (HookError.Error error: HookError.Error.values()) {
      countProduceErrors.put(error, registry.counter("produce.errors", Arrays.asList(Tag.of("error", error.value().toLowerCase()))));
    }
  }

  URI getSource(UriInfo uri) {
    return URI.create(config.getSourceHost() + "/hook/v1/");
  }

  /*
  @POST
  public Response produce(@Context HttpHeaders headers, @Context UriInfo uri, InputStream payload) throws IOException {
  */
  public Response produce(HttpHeaders headers, UriInfo uri, InputStream payload) throws IOException {
    return produce(headers, uri, "", payload);
  }

  /*
  @POST
  @Path("/{type}")
  public Response produce(@Context HttpHeaders headers, @Context UriInfo uri, @PathParam("type") String type, InputStream payload)
  */
  public Response produce(HttpHeaders headers, UriInfo uri, String type, InputStream payload)
      // if we fail to read the payload, which would be very strange
      throws IOException {
    final String id = UUID.randomUUID().toString();
    final String eventtype = config.getTypePrefix() + type;
    HookError err = new HookError();
    byte[] data = payload.readAllBytes();
    // TODO handle too large payloads
    IncomingWebhookExtension hookCustomFields = extensions.getHttp(headers, uri);
    CloudEvent event = CloudEventBuilder.v1()
        .withId(id)
        .withType(eventtype)
        .withSource(getSource(uri))
        .withExtension(extensions.getTracing(headers))
        .withData(data)
        .build();
    HookCloudEvent message = new HookCloudEvent(event, hookCustomFields);
    HookMessageKey key = new HookMessageKey();
    key.setId(id);
    key.setType(type);
    Future<RecordMetadata> resultMaybe = producer.send(key, message);
    RecordMetadata result;
    try {
      result = resultMaybe.get(limits.getProduceTimeout().toSeconds(), TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      err.setError(HookError.Error.INTERRUPTED);
      logger.error("Producer send interrupted", e);
      countProduceErrors.get(err.getError()).increment();
      return Response.serverError().entity(err).build();
    } catch (TimeoutException e) {
      err.setError(HookError.Error.TIMEOUT);
      logger.error("Producer send timeout", e);
      countProduceErrors.get(err.getError()).increment();
      return Response.serverError().entity(err).build();
    } catch (ExecutionException e) {
      if (e.getCause() != null) {
        MDC.put("cause", e.getClass().getName());
        MDC.put("message", e.getMessage());
        if (e.getCause() instanceof org.apache.kafka.common.errors.TimeoutException) {
          err.setError(HookError.Error.WRITE_TIMEOUT);
        }
      }
      MDC.put("err", err.getError().toString());
      logger.error("Producer send failed", e);
      countProduceErrors.get(err.getError()).increment();
      return Response.serverError().entity(err).build();
    }
    HookReceipt receipt = new HookReceipt();
    receipt.setId(id);
    receipt.setPartition(result.partition());
    receipt.setOffset(result.offset());
    receipt.setTimestamp(result.timestamp());
    return Response.ok(receipt, MediaType.APPLICATION_JSON).build();
  }

}
