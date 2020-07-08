package se.yolean.kafka.hook.cloudevents;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.cloudevents.CloudEventExtensions;
import io.cloudevents.Extension;

public class IncomingWebhookExtension implements Extension {

  private Map<String,String> headers = new HashMap<>(10);

  public IncomingWebhookExtension withHeader(String key, String value) {
    headers.put(key, value);
    return this;
  }

  @Override
  public Object getValue(String key) {
    return headers.get(key);
  }
  
  @Override
  public Set<String> getKeys() {
    return headers.keySet();
  }

  @Override
  public void readFrom(CloudEventExtensions extensions) {
    throw new UnsupportedOperationException("TODO this extension currently only only supports writing");
  }

}
