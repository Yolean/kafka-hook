package se.yolean.kafka.hook;

import java.time.Duration;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "limits")
public interface LimitsConfiguration {

  /**
   * In case kafka communication halts we'd want to respond before HTTP layers time out.
   */
  @WithName("produce-timeout")
  @WithDefault("20")
  Duration getProduceTimeout();

  @WithName("header-value-length-cap")
  @WithDefault("97")
  Integer getHeaderValueLengthCap();

  @WithName("header-value-ellipsis")
  @WithDefault("...")
  String getHeaderValueEllipsis();

  @WithName("payload-bytes-cap")
  @WithDefault("65536")
  Integer getPayloadBytesCap();

  @WithName("payload-bytes-ellipsis")
  @WithDefault("")
  String getPayloadBytesEllipsis();

}
