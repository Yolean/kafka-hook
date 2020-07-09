package se.yolean.kafka.hook.serializers;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CloudEventSerializer extends
    io.cloudevents.kafka.CloudEventSerializer {
}
