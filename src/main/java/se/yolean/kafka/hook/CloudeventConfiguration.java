package se.yolean.kafka.hook;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.config.ConfigProperties;

@ConfigProperties
public interface CloudeventConfiguration {

  @ConfigProperty(defaultValue = "traceparent")
  String getTraceparentHeader();
  
  @ConfigProperty(defaultValue = "tracestate")
  String getTracestateHeader();

  /**
   * @return regex for header names to exclude (matching will be case insensitive)
   */
  @ConfigProperty(defaultValue = "^(cookie)$")
  String getHttpHeadersExclude();

  @ConfigProperty(defaultValue = "97")
  Integer getHttpHeaderValueLengthCap();

  @ConfigProperty(defaultValue = "...")
  Integer getHttpHeaderValueEllipsis();

}
