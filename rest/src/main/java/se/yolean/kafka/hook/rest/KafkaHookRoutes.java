package se.yolean.kafka.hook.rest;

import java.io.IOException;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HandlerType;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import se.yolean.kafka.hook.ProduceFailed;
import se.yolean.kafka.hook.types.v1.HookReceipt;

@ApplicationScoped
public class KafkaHookRoutes {

  @Route(methods = HttpMethod.POST, regex = ".*/v1/?(.*)", type = Route.HandlerType.BLOCKING, produces = "application/json")
  public HookReceipt produce(RoutingContext ctx) throws IOException {
    String type = ctx.pathParam("param0");
    MultiMap headers = ctx.request().headers();
    @Nullable
    Buffer payload = ctx.getBody();
    return hook.produce(headers, uri, "", payload);
  }

  @Route(type = HandlerType.FAILURE, produces = "application/json")
  void unsupported(ProduceFailed e, HttpServerResponse response) {
    response.setStatusCode(500).end(e.getError());
  }

  // TODO pixy drop-in, stuff like: "/{topic: ^[^/]+$}/messages"

}
