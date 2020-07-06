package se.yolean.kafka.hook.rest;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import com.salesforce.kafka.test.listeners.PlainListener;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class KafkaHookResourceIT {

  public static final int TEST_KAFKA_PORT = 19092;

  @RegisterExtension
  static final SharedKafkaTestResource kafka = new SharedKafkaTestResource().withBrokers(1)
    .registerListener(new PlainListener().onPorts(TEST_KAFKA_PORT));

  @Test
  public void testProduceString() {
    byte[] body = "test".getBytes();
    given()
      .contentType(ContentType.TEXT)
      .accept(ContentType.JSON)
      .body(body)
      .when().post("/v1")
      .then()
        .statusCode(200)
        .body(is("TODO define receipt"));
  }

  @Disabled
  @Test
  public void testCloudeventsDistributedTracingExtensionWithEnvoyHeaders() {
    // TODO figure out how to use the DistributedTracingExtension cloudevents extension, for example given headers from envoy (so we don't try to do that in our own extension)
  }

}
