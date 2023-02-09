package se.yolean.kafka;

import java.util.Optional;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

/**
 * To be extended and decorated with an annotation like
 * <code>@ConfigProperties(prefix = "outgoing.my-channel-name")</code>
 * and then used with {@link KafkaProps}.
 */
@ConfigMapping
public interface KafkaQuarkusProducerConfig {

  @WithName("topic")
  String getTopic();

  @WithName(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)
  String getBootstrapServer();

  @WithName(ProducerConfig.ACKS_CONFIG)
  String getAcks();

  @WithName(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG)
  Boolean getEnableIdempotence();

  @WithName(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)
  Class<? extends Serializer<?>> getKeySerializer();

  @WithName(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)
  Class<? extends Serializer<?>> getValueSerializer();

  @WithName(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG)
  Optional<Integer> getRequestTimeoutMs();

  @WithName(ProducerConfig.MAX_BLOCK_MS_CONFIG)
  Optional<Integer> getMaxBlockMs();

}
