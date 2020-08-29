package se.yolean.kafka.hook.cloudevents;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.equalTo;

public class IncomingWebhookExtensionTest {

  @Test
  public void testPrefix() {
    IncomingWebhookExtension ext = new IncomingWebhookExtension("h_", 97, "...");
    ext.withHeader("x-request-id", "qwerty");
    ext.withHeader("cookie", "auth");
    assertThat(ext.getKeys(), hasItems("h_x-request-id", "h_cookie"));
    assertThat(ext.getValue("h_x-request-id"), equalTo("qwerty"));
    assertThat(ext.getValue("h_cookie"), equalTo("auth"));
  }

  @Test
  public void testCapValueLength() {
    IncomingWebhookExtension ext = new IncomingWebhookExtension("h_", 3, "..");
    ext.withHeader("x-forwarded-for", "1.42.0.41");
    assertThat(ext.getKeys(), hasItems("h_x-forwarded-for"));
    assertThat(ext.getValue("h_x-forwarded-for"), equalTo("1.4.."));
  }

  @Test
  public void testToString() {
    IncomingWebhookExtension ext = new IncomingWebhookExtension("hh_", 3, "..");
    ext.withHeader("ok", "true");
    ext.withHeader("a,b", "b,c");
    assertThat(ext.toString(), equalTo("hh_;3;..:hh_ok=tru..|hh_a,b=b,c"));
  }

  @Test
  public void testCase() {
    IncomingWebhookExtension ext = new IncomingWebhookExtension("hh_", 20, "...");
    ext.withHeader("OK", "true");
    ext.withHeader("Content-Type", "text/plain");
    assertThat(ext.getKeys(), hasItems("hh_ok", "hh_content-type"));
  }

  @Test
  public void testNoCap() {
    IncomingWebhookExtension ext = new IncomingWebhookExtension("h_", 0, null);
    ext.withHeader("x", "y");
    assertThat(ext.getKeys(), hasItems("h_x"));
  }

  @Test
  public void testCapNull() {
    IncomingWebhookExtension ext = new IncomingWebhookExtension("h_", 1, "");
    try {
      ext = new IncomingWebhookExtension("h_", 1, null);
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

}
