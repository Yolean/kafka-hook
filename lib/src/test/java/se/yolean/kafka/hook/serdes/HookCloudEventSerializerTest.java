package se.yolean.kafka.hook.serdes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Set;

import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.Test;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.kafka.CloudEventSerializer;
import se.yolean.kafka.hook.HookCloudEvent;
import se.yolean.kafka.hook.cloudevents.HookCustomFields;

class HookCloudEventSerializerTest {

  @Test
  void testSerializeStringHeadersHookCloudEvent() {
    final CloudEvent event = CloudEventBuilder.v1()
        .withId("000")
        .withType("example.demo")
        .withSource(URI.create("http://example.com"))
        .withData("text/plain","Hello world!".getBytes())
        .build();
    HookCustomFields custom = mock(HookCustomFields.class);
    when(custom.getKeys()).thenReturn(Set.of("hook_x-auth-subject", "hook_user-agent"));
    when(custom.getValueBytes("hook_x-auth-subject")).thenReturn("012".getBytes());
    when(custom.getValueBytes("hook_user-agent")).thenReturn("Test".getBytes());
    HookCloudEvent message = mock(HookCloudEvent.class);
    when(message.getEvent()).thenReturn(event);
    when(message.getCustomFields()).thenReturn(custom);
    CloudEventSerializer ceSerializer = mock(CloudEventSerializer.class);
    HookCloudEventSerializer serializer = new HookCloudEventSerializer(ceSerializer);
    Headers headers = mock(Headers.class);
    String topic = "t";
    serializer.serialize(topic, headers, message);
    verify(headers).add("hook_x-auth-subject", "012".getBytes());
    verify(headers).add("hook_user-agent", "Test".getBytes());
    verify(ceSerializer).serialize(topic, headers, message.getEvent());
    serializer.close();
  }

}
