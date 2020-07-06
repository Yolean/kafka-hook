package se.yolean.kafka.hook.rest;

import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.yolean.kafka.hooks.v1.types.Datadef;
import se.yolean.kafka.hooks.v1.types.Key;
import se.yolean.kafka.hooks.v1.types.Message;

@Produces(MediaType.APPLICATION_JSON)
@Path("/" + KafkaHookResource.API_VERSION)
public class KafkaHookResource {

  static final String API_VERSION = "v1";

  static final Logger logger = LoggerFactory.getLogger(KafkaHookResource.class);

  @POST
  public Response produce(@Context HttpHeaders headers, InputStream body) {
    return produce(headers, "", body);
  }

  @POST
  @Path("{anypath}")
  public Response produce(@Context HttpHeaders headers, @PathParam("anypath") String anypath, InputStream body) {
    Message message = new Message();
    Datadef data = new Datadef();
    message.setData(data);

    return Response.ok("TODO", MediaType.TEXT_PLAIN).build();
  }

}
