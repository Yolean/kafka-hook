package se.yolean.kafka.hook.rest;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import com.salesforce.kafka.test.listeners.PlainListener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import java.util.List;

@QuarkusTest
public class KafkaHookResourceIT {

  public static final int TEST_KAFKA_PORT = 19092;

  @RegisterExtension
  static final SharedKafkaTestResource kafka = new SharedKafkaTestResource()
    .withBrokerProperty("auto.create.topics.enable", "false")
    .withBrokers(1)
    .registerListener(new PlainListener().onPorts(TEST_KAFKA_PORT));

  @Test
  public void testProduceStringNonexistentTopic() {
    given()
      .contentType(ContentType.TEXT)
      .accept(ContentType.JSON)
      .body("test1".getBytes())
      .when().post("/v1")
      .then()
        .statusCode(500)
        .body(is("\"PRODUCE_TIMEOUT\""));
  }

  @Test
  public void testProduceString() throws UnsupportedEncodingException {
    kafka.getKafkaTestUtils().createTopic("events.stream.json", 1, (short) 1);
    given()
      .contentType(ContentType.TEXT)
      .accept(ContentType.JSON)
      .body("test2".getBytes())
      .when().post("/v1")
      .then()
        .statusCode(200)
        .body(containsString("\"partition\":0"));
    List<ConsumerRecord<byte[], byte[]>> records =
      kafka.getKafkaTestUtils().consumeAllRecordsFromTopic("events.stream.json");
    assertEquals(1, records.size(), "Should have produced one message");
    assertEquals("", new String(records.get(0).value(), "UTF-8"));
  }

  @Disabled
  @Test
  public void testCloudeventsDistributedTracingExtensionWithEnvoyHeaders() {
    // TODO figure out how to use the DistributedTracingExtension cloudevents extension, for example given headers from envoy (so we don't try to do that in our own extension)
  }

}
