package se.yolean.kafka.hook.rest;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

// TODO can we make a drop-in replacement for https://github.com/mailgun/kafka-pixy#produce ?
// Excluding /clusters endpoints

@Path("/topics")
public class PixyDropInReplacementResource {

  @POST
  @Path("/{topic: ^[^/]+$}/messages")
  public Response pixy() {
    throw new UnsupportedOperationException("Not implemented");
  }

}
