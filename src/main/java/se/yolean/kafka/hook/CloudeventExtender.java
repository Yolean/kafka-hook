package se.yolean.kafka.hook;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.extensions.DistributedTracingExtension;
import se.yolean.kafka.hook.cloudevents.IncomingWebhookExtension;

@ApplicationScoped
public class CloudeventExtender {

  @Inject CloudeventConfiguration config;

  public DistributedTracingExtension getTracing(HttpHeaders headers) {
    DistributedTracingExtension tracing = new DistributedTracingExtension();
    return tracing;
  }

  public IncomingWebhookExtension getHttp(HttpHeaders headers, UriInfo uri) {
    IncomingWebhookExtension webhook = new IncomingWebhookExtension();
    return webhook;
  }

}
