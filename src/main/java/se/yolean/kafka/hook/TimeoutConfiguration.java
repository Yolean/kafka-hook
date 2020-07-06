package se.yolean.kafka.hook;

import java.time.Duration;

import io.quarkus.arc.config.ConfigProperties;

/**
 * Settings related to how to return HTTP responses at kafka client unresponsiveness.
 */
@ConfigProperties
public interface TimeoutConfiguration {
  Duration getProduceFromHttp();
}
