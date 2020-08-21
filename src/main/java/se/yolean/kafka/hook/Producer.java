package se.yolean.kafka.hook;

import java.util.Map;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cloudevents.CloudEvent;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import se.yolean.kafka.KafkaProps;
import se.yolean.kafka.hooks.v1.types.HookMessageKey;

/**
 * This isn't meant to be an abstraction (we try to avoid Kafka client abstractions)
 * - only a means of initializing and destroying the producer.
 */
@Startup
@ApplicationScoped
public class Producer {

  static final Logger logger = LoggerFactory.getLogger(Producer.class);

  @Inject ProducerConfiguration config;

  KafkaProducer<HookMessageKey, CloudEvent> producer = null;

  void onStart(@Observes StartupEvent ev) {
    Map<String,Object> props = KafkaProps.fromQuarkusOutgoingConfig(config);
    producer = new KafkaProducer<>(props);
  }

  void onStop(@Observes ShutdownEvent ev) {
    producer.close(KafkaProps.PRODUCER_CLOSE_TIMEOUT);
  }

  public Future<RecordMetadata> send(HookMessageKey key, CloudEvent message) {
    return producer.send(new ProducerRecord<HookMessageKey,CloudEvent>(config.getTopic(), key, message));
  }

}
