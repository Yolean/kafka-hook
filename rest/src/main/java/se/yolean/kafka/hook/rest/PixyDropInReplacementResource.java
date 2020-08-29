package se.yolean.kafka.hook.rest;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

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
