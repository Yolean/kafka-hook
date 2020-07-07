package se.yolean.kafka.hook;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.extensions.DistributedTracingExtension;
import se.yolean.kafka.hook.cloudevents.IncomingWebhookExtension;

@ApplicationScoped
public class CloudEventExtender {

  public void extend(CloudEventBuilder event, HttpHeaders headers, UriInfo uri) {
    DistributedTracingExtension tracing = new DistributedTracingExtension();
    
    IncomingWebhookExtension webhook = new IncomingWebhookExtension();

  }

}
