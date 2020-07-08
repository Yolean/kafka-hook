package se.yolean.kafka.hook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.cloudevents.core.extensions.DistributedTracingExtension;
import se.yolean.kafka.hook.cloudevents.IncomingWebhookExtension;

public class CloudeventExtenderTest {

  /**
   * TODO do we need to do map
   * https://github.com/openzipkin/b3-propagation#overall-process to
   * https://w3c.github.io/trace-context/#version-format ? public void testB3() {
   * 
   * }
   */

  @Test
  public void testTracingPropagation() {
    MultivaluedMap<String, String> h = new MultivaluedMapImpl<>();
    HttpHeaders headers = mock(HttpHeaders.class);
    when(headers.getRequestHeaders()).thenReturn(h);
    h.add("p", "00-whatever");
    h.add("s", "some=otherwhatever");
    h.forEach((k, v) -> when(headers.getHeaderString(k)).thenReturn(String.join(",", v)));
    CloudeventExtender extender = new CloudeventExtender();
    extender.config = mock(CloudeventConfiguration.class);
    when(extender.config.getTraceparentHeader()).thenReturn("p");
    when(extender.config.getTracestateHeader()).thenReturn("s");
    extender.limits = mock(LimitsConfiguration.class);
    DistributedTracingExtension tracing = extender.getTracing(headers);
    assertEquals("00-whatever", tracing.getTraceparent());
    assertEquals("some=otherwhatever", tracing.getTracestate());
  }

  @Disabled // TODO there's something wrong with the mocking here
  @Test
  public void testHeaderPropagation() {
    MultivaluedMap<String, String> h = new MultivaluedMapImpl<>();
    HttpHeaders headers = mock(HttpHeaders.class);
    when(headers.getRequestHeaders()).thenReturn(h);
    h.add("x-request-id", "from-envoy-maybe");
    h.add("x-forwarded-for", "0.1.2.3456");
    h.add("cookie", "ugh");
    h.add("x-other", "secret for which we configure exclusion");
    h.forEach((k, v) -> when(headers.getHeaderString(k)).thenReturn(String.join(",", v)));
    assertEquals("from-envoy-maybe", headers.getHeaderString("x-request-id"));
    UriInfo uri = mock(UriInfo.class);
    CloudeventExtender extender = new CloudeventExtender();
    extender.config = mock(CloudeventConfiguration.class);
    when(extender.config.getHeadersExclude()).thenReturn("^(notcookie|x-other)$");
    when(extender.config.getHttpExtensionPrefix()).thenReturn("h_");
    extender.limits = mock(LimitsConfiguration.class);
    IncomingWebhookExtension http = extender.getHttp(headers, uri);
    assertThat(http.getKeys(), hasItems("h_x-request-id", "h_x-forwarded-for", "h_cookie"));
    assertEquals("from-envoy-maybe", http.getValue("h_x-request-id"));
    assertEquals("0.1.2.3456", http.getValue("h_x-forwarded-for"));
    assertEquals("ugh", http.getValue("h_cookie"));
    assertEquals(3, http.getKeys().size(), "No more headers expected");
  }

}
