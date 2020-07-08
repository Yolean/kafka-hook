package se.yolean.kafka.hook.rest;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import com.salesforce.kafka.test.listeners.PlainListener;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@QuarkusTest
public class KafkaHookResourceIntegrationTest {

  public static final int TEST_KAFKA_PORT = 19092;

  private AdminClient adminClient = null;
  private KafkaConsumer<String, String> consumer = null;
  private TopicPartition tp = null;
  private Duration pollTime = Duration.ofMillis(100);
  private long startOffset = 0;

  @RegisterExtension
  static final SharedKafkaTestResource kafka = new SharedKafkaTestResource()
      .withBrokerProperty("auto.create.topics.enable", "false")
      .withBrokers(1)
      .registerListener(new PlainListener().onPorts(TEST_KAFKA_PORT));

  @BeforeEach
  public void kafkaOpen() throws Exception {
    NewTopic newTopic = new NewTopic("events.stream.json", 1, (short) 1);
    tp = new TopicPartition(newTopic.name(), 0);
     Map<String, Object> adminProps = new HashMap<>(10);
    adminProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + TEST_KAFKA_PORT);
    adminClient = AdminClient.create(adminProps);
    ListTopicsResult topics = adminClient.listTopics();
    if (!topics.names().get().contains(newTopic.name())) {
      CreateTopicsResult create = adminClient.createTopics(Arrays.asList(newTopic));
      assertEquals(1, create.numPartitions(tp.topic()).get());
      waitBetweenPolls();
    }
    Map<String, Object> props = new HashMap<>(10);
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + TEST_KAFKA_PORT);
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, this.getClass().getName() + System.currentTimeMillis());
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    consumer = new KafkaConsumer<>(props);
    startOffset = consumer.endOffsets(List.of(tp)).getOrDefault(tp, 0L);
    consumer.assign(Arrays.asList(tp));
    consumer.seek(tp, startOffset);
    assertEquals(0, consumer.poll(Duration.ofMillis(1)).count());
    consumer.commitSync();
  }

  @AfterEach
  public void kafkaClose() {
    consumer.close();
    adminClient.close();
  }

  // Maybe test robustness depends on how we poll and leave idle time between
  // polls
  private void waitBetweenPolls() {
    try {
      Thread.sleep(pollTime.toMillis());
    } catch (Exception e) {
      throw new RuntimeException("What's happening", e);
    }
  }

  private Map<String,String> headers(ConsumerRecord<?, ?> record) {
    Map<String,String> h = new HashMap<>();
    for (Header header : record.headers()) {
      h.put(header.key(), new String(header.value()));
    }
    return h;
  }

  @Disabled // need to find a way to change topic name
  @Test
  public void testProduceStringNonexistentTopic() {
    given()
      .contentType(ContentType.TEXT)
      .accept(ContentType.JSON)
      .body("test0".getBytes())
      .when().post("/v1")
      .then()
        .body(is("{\"error\":\"WRITE_TIMEOUT\"}"))
        .statusCode(500);
  }

  @Test
  public void testProduceString() throws UnsupportedEncodingException {
    given()
      .contentType(ContentType.TEXT)
      .accept(ContentType.JSON)
      .body("test1".getBytes())
      .when().post("/v1")
      .then()
        .body(containsString("\"partition\":0"))
        .body(containsString("\"offset\":" + (startOffset)))
        .statusCode(200);
    given()
      .contentType(ContentType.TEXT)
      .accept(ContentType.JSON)
      .header("x-forwarded-for", "127.0.0.1")
      .header("cookie", "long string with auth stuff")
      .body("test2".getBytes())
      .when().post("/v1")
      .then()
        .body(containsString("\"offset\":" + (startOffset + 1)))
        .statusCode(200);
    waitBetweenPolls();
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
    assertEquals(2, records.count(), "Should have produced two messages");
    Iterator<ConsumerRecord<String, String>> it = records.iterator();
    ConsumerRecord<String, String> record1 = it.next();
    ConsumerRecord<String, String> record2 = it.next();
    // https://github.com/cloudevents/spec/blob/bf71de532ca3e69ab2fe5854a1f31f647894dae4/kafka-protocol-binding.md#325-example
    assertEquals("test1", record1.value());
    assertEquals("test2", record2.value());
    consumer.commitSync();
    assertThat(headers(record2).keySet(), hasItems("ce_xyhttp_x-forwarded-for"));
    assertEquals("127.0.0.1", headers(record2).get("ce_xyhttp_x-forwarded-for"));
    assertThat(headers(record2).keySet(), not(hasItems("ce_xyhttp_cookie")));
  }

  @Test
  public void testCloudeventsDistributedTracingExtensionWithEnvoyHeaders() {
    given()
      .contentType(ContentType.TEXT)
      .accept(ContentType.JSON)
      .header("traceparent","00-...")
      .header("tracestate", "test=...")
      .body("test1".getBytes())
      .when().post("/v1")
      .then()
        .body(containsString("\"partition\":0"))
        .body(containsString("\"offset\":" + (startOffset)))
        .statusCode(200);
    waitBetweenPolls();
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
    assertEquals(1, records.count(), "Should have produced one message");
    ConsumerRecord<String, String> record1 = records.iterator().next();
    assertEquals("00-...", headers(record1).get("ce_traceparent"));
    assertEquals("test=...", headers(record1).get("ce_tracestate"));
  }

}
