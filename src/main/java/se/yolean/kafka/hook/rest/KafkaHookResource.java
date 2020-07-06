package se.yolean.kafka.hook.rest;

import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Produces(MediaType.APPLICATION_JSON)
@Path("/messages")
public class KafkaHookResource {

  static final Logger logger = LoggerFactory.getLogger(KafkaHookResource.class);

  @POST
  public Response produce(@Context HttpHeaders headers, InputStream body) {

    return Response.ok("TODO", MediaType.TEXT_PLAIN).build();
  }

}
