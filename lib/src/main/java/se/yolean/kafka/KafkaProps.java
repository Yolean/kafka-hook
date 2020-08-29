package se.yolean.kafka;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;

public abstract class KafkaProps {

  public static final Duration PRODUCER_CLOSE_TIMEOUT = Duration.ofSeconds(10);

  static public Map<String,Object> fromQuarkusOutgoingConfig(KafkaQuarkusProducerConfig c) {
    Map<String, Object> props = new HashMap<>(10);
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, c.getBootstrapServer());
    props.put(ProducerConfig.ACKS_CONFIG, c.getAcks());
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, c.getEnableIdempotence());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, c.getKeySerializer());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, c.getValueSerializer());
    if (c.getRequestTimeoutMs().isPresent()) {
      props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, c.getRequestTimeoutMs().orElseThrow());
    }
    if (c.getMaxBlockMs().isPresent()) {
      props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, c.getMaxBlockMs().orElseThrow());
    }
    return props;
  }

}
