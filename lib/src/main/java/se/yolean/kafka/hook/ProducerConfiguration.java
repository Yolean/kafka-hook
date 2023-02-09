package se.yolean.kafka.hook;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;

import io.cloudevents.CloudEvent;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import se.yolean.kafka.KafkaQuarkusProducerConfig;
import se.yolean.kafka.hook.types.v1.HookMessageKey;

@ConfigMapping(prefix = "outgoing.hook")
public interface ProducerConfiguration extends KafkaQuarkusProducerConfig {

  @WithName(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)
  Class<? extends Serializer<HookMessageKey>> getKeySerializer();

  @WithName(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)
  Class<? extends Serializer<CloudEvent>> getValueSerializer();

}
