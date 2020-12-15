package se.yolean.kafka.hook;

import io.cloudevents.CloudEvent;
import se.yolean.kafka.hook.cloudevents.HookCustomFields;
import se.yolean.kafka.hook.cloudevents.IncomingWebhookExtension;

public class HookCloudEvent {

  private final CloudEvent event;
  private final IncomingWebhookExtension hook;

  public HookCloudEvent(CloudEvent event, IncomingWebhookExtension hookCustomFields) {
    this.event = event;
    this.hook = hookCustomFields;
  }

  public CloudEvent getEvent() {
    return event;
  }

  public HookCustomFields getCustomFields() {
    return hook;
  }

}
