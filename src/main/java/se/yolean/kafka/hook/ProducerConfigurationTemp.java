package se.yolean.kafka.hook;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
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
    Map<String, Object> props = new HashMap<>(10);
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, c.getBootstrapServer());
    props.put(ProducerConfig.ACKS_CONFIG, c.getAcks());
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, c.getEnableIdempotence());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, c.getKeySerializer());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, c.getVallueSerializer());
    if (c.getDeliveryTimeoutMs().isPresent()) {
      props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, c.getDeliveryTimeoutMs().orElseThrow());
    }
    if (c.getLingerMs().isPresent()) {
      props.put(ProducerConfig.LINGER_MS_CONFIG, c.getLingerMs().orElseThrow());
    }
    if (c.getRequestTimeoutMs().isPresent()) {
      props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, c.getRequestTimeoutMs().orElseThrow());
    }
    if (c.getConnectionMaxIdleMs().isPresent()) {
      props.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, c.getConnectionMaxIdleMs().orElseThrow());
    }
    return props;
  }

}
