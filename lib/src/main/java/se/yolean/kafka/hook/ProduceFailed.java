package se.yolean.kafka.hook;

import se.yolean.kafka.hook.types.v1.HookError;


/**
 * Means we failed to fulfil the primary responsibility of kafka-hook.
 */
public class ProduceFailed extends Exception {

  private static final long serialVersionUID = 1L;

  private final HookError error;

  public ProduceFailed(HookError error) {
    this.error = error;
  }

  public HookError getError() {
    return error;
  }

}
