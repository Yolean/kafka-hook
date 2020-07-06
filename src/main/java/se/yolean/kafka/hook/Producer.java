package se.yolean.kafka.hook;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class Producer {

  static final Logger logger = LoggerFactory.getLogger(Producer.class);

  @Inject ProducerConfiguration config;

  void onStart(@Observes StartupEvent ev) {               
    
  }

  void onStop(@Observes ShutdownEvent ev) {               
    
  }

}
