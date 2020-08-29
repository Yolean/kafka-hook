package se.yolean.kafka.hook.http;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Test;

import se.yolean.kafka.hook.CloudeventConfiguration;

/**
 * Note that integration testing (with REST) is done in the rest package, not the lib.
 * The lib uses only unit tests, not quarkus of embedded-kafka tests.
 */
class KafkaHookResourceTest {

  @Test
  void testGetSourceInitialHardcodedBehaviorThatShouldChange() {
	KafkaHookResource resource = new KafkaHookResource();
	resource.config = mock(CloudeventConfiguration.class);
	when(resource.config.getSourceHost()).thenReturn("example.com");
	UriInfo uri = mock(UriInfo.class);
	assertEquals("example.com/hook/v1/", resource.getSource(uri).toString());
  }

}
