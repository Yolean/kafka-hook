package se.yolean.kafka.hook;

import java.util.Set;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import io.cloudevents.core.extensions.DistributedTracingExtension;
import se.yolean.kafka.hook.cloudevents.IncomingWebhookExtension;

@ApplicationScoped
public class CloudeventExtender {

  @Inject
  CloudeventConfiguration config;
  @Inject
  LimitsConfiguration limits;

  Pattern getHeadersExcludeRegex() {
    return Pattern.compile(config.getHeadersExclude(),
      Pattern.CASE_INSENSITIVE);
  }

  public DistributedTracingExtension getTracing(HttpHeaders headers) {
    DistributedTracingExtension tracing = new DistributedTracingExtension();
    String parent = headers.getHeaderString(config.getTraceparentHeader());
    if (parent != null && parent.length() > 0)
      tracing.setTraceparent(parent);
    String state = headers.getHeaderString(config.getTracestateHeader());
    if (state != null && state.length() > 0)
      tracing.setTracestate(state);
    return tracing;
  }

  public IncomingWebhookExtension getHttp(HttpHeaders headers, UriInfo uri) {
    IncomingWebhookExtension webhook = new IncomingWebhookExtension();
    Set<String> keys = headers.getRequestHeaders().keySet();
    for (String key : keys) {
      if (!getHeadersExcludeRegex().matcher(key).matches()) {
        webhook.withHeader(key.toLowerCase(), headers.getHeaderString(key));
      }
    }
    return webhook;
  }

}
