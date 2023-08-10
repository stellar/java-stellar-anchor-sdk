package org.stellar.anchor.platform.event;

import org.stellar.anchor.api.event.AnchorEvent;

public abstract class EventHandler {
  abstract void handleEvent(AnchorEvent event);
}
