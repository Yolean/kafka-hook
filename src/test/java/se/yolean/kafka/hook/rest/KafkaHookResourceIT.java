package se.yolean.kafka.hook.rest;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import com.salesforce.kafka.test.listeners.PlainListener;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class KafkaHookResourceIT {

  public static final int TEST_KAFKA_PORT = 19092;

  @RegisterExtension
  static final SharedKafkaTestResource kafka = new SharedKafkaTestResource().withBrokers(1)
    .registerListener(new PlainListener().onPorts(TEST_KAFKA_PORT));

  
  
}
