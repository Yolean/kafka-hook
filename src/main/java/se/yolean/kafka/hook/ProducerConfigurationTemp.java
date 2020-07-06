package se.yolean.kafka.hook;

import java.time.Duration;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;

/**
 * Can be moved into ProducerConfiguration with Quarkus 1.6
 * - https://github.com/quarkusio/quarkus/pull/9771
 */
public interface ProducerConfigurationTemp {
  
  /*@ConfigIgnore*/ static Duration PRODUCER_CLOSE_TIMEOUT = Duration.ofSeconds(10);

  // The following static blocks need to be maintained together with the ConfigProperty items below
  /*@ConfigIgnore*/ static Class<?>[] KNOWN_SERIALIZERS = { // Native complie needs to know about these classes
    io.quarkus.kafka.client.serialization.ObjectMapperSerializer.class,
    io.cloudevents.kafka.CloudEventSerializer.class
  };

  /*@ConfigIgnore*/ static Map<String,Object> toProps(ProducerConfiguration c) {
    return Map.of(
      ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, c.getBootstrapServer(),
      ProducerConfig.ACKS_CONFIG, c.getAcks(),
      ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, c.getEnableIdempotence(),
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, c.getKeySerializer(),
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, c.getVallueSerializer()
    );
  }

}