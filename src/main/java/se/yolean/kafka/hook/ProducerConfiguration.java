package se.yolean.kafka.hook;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.config.ConfigProperties;

@ConfigProperties(prefix = "kafka")
public interface ProducerConfiguration {

  @ConfigProperty(name = "topic")
  String getTopic();

  @ConfigProperty(name = ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)
  String getBootstrapServer();

  @ConfigProperty(name = ProducerConfig.ACKS_CONFIG)
  String getAcks();

  @ConfigProperty(name = ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG)
  Boolean getEnableIdempotence();
  
}
