package se.yolean.kafka.hook;

import java.time.Duration;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.config.ConfigProperties;

@ConfigProperties
public interface LimitsConfiguration {

  /**
   * In case kafka communication halts we'd want to respond before HTTP layers time out.
   */
  @ConfigProperty(defaultValue = "20")
  Duration getProduceTimeout();

  @ConfigProperty(defaultValue = "97")
  Integer getHeaderValueLengthCap();

  @ConfigProperty(defaultValue = "...")
  Integer getHeaderValueEllipsis();

  @ConfigProperty(defaultValue = "65536")
  Integer getPayloadBytesCap();

  @ConfigProperty(defaultValue = "")
  Integer getPayloadBytesEllipsis();

}
