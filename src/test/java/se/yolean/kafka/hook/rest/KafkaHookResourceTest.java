package se.yolean.kafka.hook.rest;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class KafkaHookResourceTest {

  @Test
  public void testMessageWithKafkaUnavailable() {
    byte[] body = "test".getBytes();
    given()
      .contentType(ContentType.TEXT)
      //.accept(ContentType.TEXT) // You get a 406 if path + content-type is right but accept isn't a @Produces
      .body(body)
      .when().post("/messages") // You get a 405 if there's the method isn't supported on this path
      .then()
        .statusCode(500)
        .body(is("Failed to submit message"));
  }

}
