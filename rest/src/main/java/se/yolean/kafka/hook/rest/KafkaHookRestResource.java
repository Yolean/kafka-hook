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
@Path("/")
public class KafkaHookRestResource {

  @Inject KafkaHookResource hook;

  @POST
  @Path("hook/v1")
  public Response produce(@Context HttpHeaders headers, @Context UriInfo uri, InputStream payload)
      throws IOException {
    return hook.produce(headers, uri, "", payload);
  }

  @POST
  @Path("hook/v1/{type:.*}")
  public Response produce(@Context HttpHeaders headers, @Context UriInfo uri, @PathParam("type") String type, InputStream payload)
      throws IOException {
    return hook.produce(headers, uri, type, payload);
  }

  @POST
  @Path("{prefix}/v1/hook")
  public Response produceAltPathWithoutType(@Context HttpHeaders headers, @Context UriInfo uri, InputStream payload)
      throws IOException {
    return hook.produce(headers, uri, "", payload);
  }

  @POST
  @Path("{prefix}/v1/hook/{type:.*}")
  public Response produceAltPath(@Context HttpHeaders headers, @Context UriInfo uri, @PathParam("type") String type, InputStream payload)
      throws IOException {
    return hook.produce(headers, uri, type, payload);
  }

}
