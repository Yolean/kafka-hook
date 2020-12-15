package se.yolean.kafka.hook.cloudevents;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Note that this "extension" fails to produce field names that comply with
 * https://github.com/cloudevents/spec/blob/master/spec.md#attribute-naming-convention
 */
public class IncomingWebhookExtension implements HookCustomFields { //io.cloudevents.Extension {

  // @Override
  // public void readFrom(CloudEventExtensions extensions) {
  //   throw new UnsupportedOperationException("TODO this extension currently only only supports writing");
  // }

  private Map<String,String> fields = new LinkedHashMap<>(10);

  private final String prefix;

  private int capValue;

  private String capValueEllipsis;

  public IncomingWebhookExtension(
      String prefixAfterCloudeventsPrefix,
      int capValueLength,
      String capValueLengthEllipsis) {
    this.prefix = prefixAfterCloudeventsPrefix;
    this.capValue = capValueLength;
    this.capValueEllipsis = capValueLengthEllipsis;
    if (this.capValue > 0 && capValueEllipsis == null) throw new IllegalArgumentException("Value length cap is enabled but ellipsis is null (empty string is allowed)");
  }

  public IncomingWebhookExtension withHeader(String headerName, String value) {
    if (value == null) throw new IllegalArgumentException("Unexpected null value for header " + headerName);
    headerName = headerName.toLowerCase();
    if (capValue > 0 && value.length() > capValue) {
      value = value.substring(0, capValue) + capValueEllipsis;
    }
    fields.put(this.prefix + headerName, value);
    return this;
  }

  @Override
  public Object getValue(String key) {
    return fields.get(key);
  }

  @Override
  public byte[] getValueBytes(String key) {
    return fields.get(key).getBytes();
  }

  @Override
  public Set<String> getKeys() {
    return fields.keySet();
  }

  @Override
  public String toString() {
    if (fields.size() == 0) return prefix;
    StringBuilder s = new StringBuilder();
    s.append(this.prefix);
    s.append(';').append(this.capValue).append(';').append(this.capValueEllipsis);
    s.append(':');
    fields.forEach((k, v) -> s.append(k).append('=').append(v).append('|'));
    return s.toString().substring(0, s.length() - 1);
  }

}
