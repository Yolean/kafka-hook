package se.yolean.kafka.hook;

import java.util.Optional;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.cloudevents.CloudEvent;
import io.quarkus.arc.config.ConfigProperties;
import se.yolean.kafka.KafkaQuarkusProducerConfig;
import se.yolean.kafka.hook.types.v1.HookMessageKey;

@ConfigProperties(prefix = "outgoing.hook")
public interface ProducerConfiguration extends KafkaQuarkusProducerConfig {

  @ConfigProperty(name = ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)
  Class<? extends Serializer<HookMessageKey>> getKeySerializer();

  @ConfigProperty(name = ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)
  Class<? extends Serializer<CloudEvent>> getValueSerializer();

}
