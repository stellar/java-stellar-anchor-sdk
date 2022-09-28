package org.stellar.anchor.event;

import org.stellar.anchor.event.models.AnchorEvent;

public abstract class EventPublisher {
  EventService eventService;

  public abstract void publish(AnchorEvent event);

  public EventService getEventService() {
    return eventService;
  }

  public void setEventService(EventService eventService) {
    this.eventService = eventService;
  }
}
