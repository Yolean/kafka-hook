package se.yolean.kafka.hook.serdes;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import io.cloudevents.kafka.CloudEventSerializer;
import io.quarkus.runtime.annotations.RegisterForReflection;
import se.yolean.kafka.hook.HookCloudEvent;
import se.yolean.kafka.hook.cloudevents.HookCustomFields;

@RegisterForReflection
public class HookCloudEventSerializer implements Serializer<HookCloudEvent> {

  final CloudEventSerializer ce;

  public HookCloudEventSerializer() {
    this(new CloudEventSerializer());
  }

  HookCloudEventSerializer(CloudEventSerializer ce) {
    this.ce = ce;
  }

  @Override
  public byte[] serialize(String topic, HookCloudEvent data) {
    throw new UnsupportedOperationException("CloudEventSerializer supports only the signature serialize(String, Headers, CloudEvent)");
  }

  @Override
  public byte[] serialize(String topic, Headers headers, HookCloudEvent data) {
    HookCustomFields hook = data.getCustomFields();
    for (String key : hook.getKeys()) {
      headers.add(key, hook.getValueBytes(key));
    }
    return ce.serialize(topic, headers, data.getEvent());
  }

}
