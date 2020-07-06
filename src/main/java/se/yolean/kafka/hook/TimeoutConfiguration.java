package se.yolean.kafka.hook;

/**
 * Settings related to how to return HTTP responses at kafka client unresponsiveness.
 */
public interface TimeoutConfiguration {
  int getSeconds();
}
