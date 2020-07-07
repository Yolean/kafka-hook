package se.yolean.kafka.hook.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.ApplicationScoped;
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
import se.yolean.kafka.hook.CloudeventExtender;
import se.yolean.kafka.hook.Producer;
import se.yolean.kafka.hook.LimitsConfiguration;
import se.yolean.kafka.hook.cloudevents.IncomingWebhookExtension;
import se.yolean.kafka.hooks.v1.types.HookError;
import se.yolean.kafka.hooks.v1.types.Key;
import se.yolean.kafka.hooks.v1.types.Receipt;

@Produces(MediaType.APPLICATION_JSON)
@Path("/" + KafkaHookResource.API_VERSION)
public class KafkaHookResource {

  static final String API_VERSION = "v1";

  static final Logger logger = LoggerFactory.getLogger(KafkaHookResource.class);

  @Inject Producer producer;
  @Inject LimitsConfiguration limits;
  @Inject CloudeventExtender extensions;

  @POST
  public Response produce(@Context HttpHeaders headers, @Context UriInfo uri, InputStream payload) throws IOException {
    return produce(headers, uri, "", payload);
  }

  @POST
  @Path("{anypath}")
  public Response produce(@Context HttpHeaders headers, @Context UriInfo uri, @PathParam("anypath") String anypath, InputStream payload)
      // if we fail to read the payload, which would be very strange
      throws IOException {
    HookError err = new HookError();
    byte[] data = payload.readAllBytes();
    // TODO handle too large payloads
    CloudEvent message = CloudEventBuilder.v1()
        .withId("hello")
        .withType("example.kafka")
        .withSource(URI.create("http://localhost"))
        .withExtension(extensions.getTracing(headers))
        .withExtension(extensions.getHttp(headers, uri))
        .withData(data)
        .build();
    Key key = new Key();
    Future<RecordMetadata> resultMaybe = producer.send(key, message);
    RecordMetadata result;
    try {
      result = resultMaybe.get(limits.getProduceTimeout().toSeconds(), TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      logger.error("Producer send interrupted", e);
      return Response.serverError().entity(err).build();
    } catch (TimeoutException e) {
      logger.error("Producer send timeout", e);
      err.setError(HookError.Error.TIMEOUT);
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
    Receipt receipt = new Receipt();
    receipt.setPartition(result.partition());
    receipt.setOffset(result.offset());
    receipt.setTimestamp(result.timestamp());
    return Response.ok(receipt, MediaType.APPLICATION_JSON).build();
  }

}
