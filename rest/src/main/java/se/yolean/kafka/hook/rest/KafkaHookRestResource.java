package se.yolean.kafka.hook.rest;

import java.io.IOException;
import java.io.InputStream;

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

import se.yolean.kafka.hook.http.KafkaHookResource;

@Produces(MediaType.APPLICATION_JSON)
@Path("/hook/v1")
public class KafkaHookRestResource {

  @Inject KafkaHookResource hook;

  @POST
  public Response produce(@Context HttpHeaders headers, @Context UriInfo uri, InputStream payload) throws IOException {
    return hook.produce(headers, uri, "", payload);
  }

  @POST
  @Path("/{type}")
  public Response produce(@Context HttpHeaders headers, @Context UriInfo uri, @PathParam("type") String type, InputStream payload)
      // if we fail to read the payload, which would be very strange
      throws IOException {
    return hook.produce(headers, uri, type, payload);
  }

}
