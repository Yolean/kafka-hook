package se.yolean.kafka.hook;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "cloudevent")
public interface CloudeventConfiguration {

  @WithName("type-prefix")
  String getTypePrefix();

  @WithName("source-host")
  String getSourceHost();

  @WithName("traceparent-header")
  @WithDefault("traceparent")
  String getTraceparentHeader();

  @WithName("tracestate-header")
  @WithDefault("tracestate")
  String getTracestateHeader();

  @WithName("http-extension-prefix")
  @WithDefault("hook_")
  String getHttpExtensionPrefix();

  /**
   * @return regex for header names to exclude (matching will be case insensitive)
   */
  @WithName("headers-exclude")
  @WithDefault("^(cookie)$")
  String getHeadersExclude();

}
