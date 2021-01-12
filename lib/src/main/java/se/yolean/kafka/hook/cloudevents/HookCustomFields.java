package se.yolean.kafka.hook.cloudevents;

import java.util.Set;

public interface HookCustomFields {

  public Set<String> getKeys();

  public Object getValue(String key);

  /**
   * The CloudEvents spec handles typed values, but here we deal with HTTP headers.
   * @param key from {@link #getKeys()}
   * @return the string value as bytes
   */
  byte[] getValueBytes(String key);

}
