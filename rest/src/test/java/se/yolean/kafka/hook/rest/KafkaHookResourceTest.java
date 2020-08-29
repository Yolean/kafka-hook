package se.yolean.kafka.hook.rest;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
public class KafkaHookResourceTest {

  @Test
  public void testMessageWithKafkaUnavailable() {
    byte[] body = "test".getBytes();
    given()
      .contentType(ContentType.TEXT)
      .accept(ContentType.JSON) // TODO will the v1 API care about Accept?
      .body(body)
      .when().post("/hook/v1") // You get a 405 if there's the method isn't supported on this path
      .then()
        .statusCode(500)
        //.body(is("{\"error\":\"WRITE_TIMEOUT\"}"));
        .body(containsString("TIMEOUT"));
  }

}
