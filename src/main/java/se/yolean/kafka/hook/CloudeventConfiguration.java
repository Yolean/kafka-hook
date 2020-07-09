package se.yolean.kafka.hook;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.config.ConfigProperties;

@ConfigProperties
public interface CloudeventConfiguration {

  String getTypeFixed();

  String getSourceHost();

  @ConfigProperty(defaultValue = "traceparent")
  String getTraceparentHeader();
  
  @ConfigProperty(defaultValue = "tracestate")
  String getTracestateHeader();

  @ConfigProperty(defaultValue = "xyhttp_")
  String getHttpExtensionPrefix();

  /**
   * @return regex for header names to exclude (matching will be case insensitive)
   */
  @ConfigProperty(defaultValue = "^(cookie)$")
  String getHeadersExclude();

}
