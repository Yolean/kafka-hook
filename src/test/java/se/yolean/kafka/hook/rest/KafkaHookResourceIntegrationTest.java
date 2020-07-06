package se.yolean.kafka.hook.rest;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import com.salesforce.kafka.test.listeners.PlainListener;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@QuarkusTest
public class KafkaHookResourceIntegrationTest {

  public static final int TEST_KAFKA_PORT = 19092;

  static KafkaConsumer<String, String> consumer = null;
  static AdminClient adminClient = null;

  @RegisterExtension
  static final SharedKafkaTestResource kafka = new SharedKafkaTestResource()
      .withBrokerProperty("auto.create.topics.enable", "false").withBrokers(1)
      .registerListener(new PlainListener().onPorts(TEST_KAFKA_PORT));

  @BeforeEach
  public void kafkaOpen() throws Exception {
    Map<String, Object> props = new HashMap<>(10);
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + TEST_KAFKA_PORT);
    adminClient = AdminClient.create(props);
    adminClient.listTopics();
    CreateTopicsResult result = adminClient
        .createTopics(Arrays.asList(new NewTopic("events.stream.json", 1, (short) 1)));
    result.all().get();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + TEST_KAFKA_PORT);
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    //props.put(ConsumerConfig.GROUP_ID_CONFIG, this.getClass().getName());
    //props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumer = new KafkaConsumer<>(props);
  }

  @AfterEach
  public void kafkaClose() {
    consumer.close();
    adminClient.close();
  }

  @Disabled // need to find a way to change topic name
  @Test
  public void testProduceStringNonexistentTopic() {
    given().contentType(ContentType.TEXT).accept(ContentType.JSON).body("test1".getBytes()).when().post("/v1").then()
        .statusCode(500).body(is("{\"error\":\"WRITE_TIMEOUT\"}"));
  }

  @Test
  public void testProduceString() throws UnsupportedEncodingException {
    given()
      .contentType(ContentType.TEXT)
      .accept(ContentType.JSON)
      .body("test2".getBytes())
      .when().post("/v1")
      .then()
        .body(containsString("\"partition\":0"))
        .body(containsString("\"offset\":0"))
        .statusCode(200);
    given()
      .contentType(ContentType.TEXT)
      .accept(ContentType.JSON)
      .body("test3".getBytes())
      .when().post("/v1")
      .then()
        .body(containsString("\"offset\":1"))
        .statusCode(200);
    try { Thread.sleep(60000); } catch (Exception e) {}
    consumer.subscribe(Arrays.asList("events.stream.json"));
    assertEquals(0, consumer.poll(Duration.ofMillis(1)).count());
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
    assertEquals(2, records.count(), "Should have produced one message");
    assertEquals("", records.iterator().next().value());
  }

  @Disabled
  @Test
  public void testCloudeventsDistributedTracingExtensionWithEnvoyHeaders() {
    // TODO figure out how to use the DistributedTracingExtension cloudevents extension, for example given headers from envoy (so we don't try to do that in our own extension)
  }

}
