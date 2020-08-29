package se.yolean.kafka.hook.serdes;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CloudEventDeserializer extends
    io.cloudevents.kafka.CloudEventDeserializer {
}
