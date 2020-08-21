package se.yolean.kafka.hook;

import java.util.Optional;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.cloudevents.CloudEvent;
import io.quarkus.arc.config.ConfigProperties;
import se.yolean.kafka.hooks.v1.types.HookMessageKey;

@ConfigProperties(prefix = "outgoing.hook")
public interface ProducerConfiguration {

  @ConfigProperty(name = "topic")
  String getTopic();

  @ConfigProperty(name = ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)
  String getBootstrapServer();

  @ConfigProperty(name = ProducerConfig.ACKS_CONFIG)
  String getAcks();

  @ConfigProperty(name = ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG)
  Boolean getEnableIdempotence();

  @ConfigProperty(name = ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)
  Class<? extends Serializer<HookMessageKey>> getKeySerializer();

  @ConfigProperty(name = ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)
  Class<? extends Serializer<CloudEvent>> getValueSerializer();

  @ConfigProperty(name = ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG)
  Optional<Integer> getRequestTimeoutMs();

  @ConfigProperty(name = ProducerConfig.MAX_BLOCK_MS_CONFIG)
  Optional<Integer> getMaxBlockMs();

}
