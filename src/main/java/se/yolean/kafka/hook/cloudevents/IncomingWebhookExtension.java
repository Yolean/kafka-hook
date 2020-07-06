package se.yolean.kafka.hook.cloudevents;

import java.util.Set;

import io.cloudevents.CloudEventExtensions;
import io.cloudevents.Extension;

public class IncomingWebhookExtension implements Extension {

  @Override
  public Object getValue(String key) {
    if ("todo".equals(key)) {
      return new Object();
    }
    throw new UnsupportedOperationException("TODO key: " + key);
  }
  
  @Override
  public Set<String> getKeys() {
    return Set.of("todo");
  }

  @Override
  public void readFrom(CloudEventExtensions extensions) {
    throw new UnsupportedOperationException("TODO implement");
  }

}
