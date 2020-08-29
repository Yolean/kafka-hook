package se.yolean.kafka.hook.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
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
import se.yolean.kafka.hook.CloudeventConfiguration;
import se.yolean.kafka.hook.CloudeventExtender;
import se.yolean.kafka.hook.Producer;
import se.yolean.kafka.hook.LimitsConfiguration;
import se.yolean.kafka.hook.types.v1.HookError;
import se.yolean.kafka.hook.types.v1.HookMessageKey;
import se.yolean.kafka.hook.types.v1.HookReceipt;

@Produces(MediaType.APPLICATION_JSON)
@Path("/hook/v1")
public class KafkaHookResource {

  static final Logger logger = LoggerFactory.getLogger(KafkaHookResource.class);

  @Inject Producer producer;
  @Inject CloudeventConfiguration config;
  @Inject LimitsConfiguration limits;
  @Inject CloudeventExtender extensions;

  URI getSource(UriInfo uri) {
    return URI.create(config.getSourceHost() + "/hook/v1/");
  }

  @POST
  public Response produce(@Context HttpHeaders headers, @Context UriInfo uri, InputStream payload) throws IOException {
    return produce(headers, uri, "", payload);
  }

  @POST
  @Path("/{type}")
  public Response produce(@Context HttpHeaders headers, @Context UriInfo uri, @PathParam("type") String type, InputStream payload)
      // if we fail to read the payload, which would be very strange
      throws IOException {
    final String id = UUID.randomUUID().toString();
    final String eventtype = config.getTypePrefix() + type;
    HookError err = new HookError();
    byte[] data = payload.readAllBytes();
    // TODO handle too large payloads
    CloudEvent message = CloudEventBuilder.v1()
        .withId(id)
        .withType(eventtype)
        .withSource(getSource(uri))
        .withExtension(extensions.getTracing(headers))
        .withExtension(extensions.getHttp(headers, uri))
        .withData(data)
        .build();
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
      return Response.serverError().entity(err).build();
    } catch (TimeoutException e) {
      err.setError(HookError.Error.TIMEOUT);
      logger.error("Producer send timeout", e);
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
