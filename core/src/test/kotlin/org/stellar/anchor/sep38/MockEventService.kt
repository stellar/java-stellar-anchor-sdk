package org.stellar.anchor.sep38

import org.stellar.anchor.event.EventService
import org.stellar.anchor.event.models.AnchorEvent

class MockEventService : EventService {
  override fun publish(event: AnchorEvent?) {
    TODO("Not implemented! Use it with mockk in your tests")
  }
}
