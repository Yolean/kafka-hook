package se.yolean.kafka;

import java.util.Optional;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.config.ConfigProperties;

/**
 * To be extended and decorated with an annotation like
 * <code>@ConfigProperties(prefix = "outgoing.my-channel-name")</code>
 * and then used with {@link KafkaProps}.
 */
public interface KafkaQuarkusProducerConfig {

  @ConfigProperty(name = "topic")
  String getTopic();

  @ConfigProperty(name = ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)
  String getBootstrapServer();

  @ConfigProperty(name = ProducerConfig.ACKS_CONFIG)
  String getAcks();

  @ConfigProperty(name = ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG)
  Boolean getEnableIdempotence();

  @ConfigProperty(name = ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)
  Class<? extends Serializer<?>> getKeySerializer();

  @ConfigProperty(name = ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)
  Class<? extends Serializer<?>> getValueSerializer();

  @ConfigProperty(name = ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG)
  Optional<Integer> getRequestTimeoutMs();

  @ConfigProperty(name = ProducerConfig.MAX_BLOCK_MS_CONFIG)
  Optional<Integer> getMaxBlockMs();

}
