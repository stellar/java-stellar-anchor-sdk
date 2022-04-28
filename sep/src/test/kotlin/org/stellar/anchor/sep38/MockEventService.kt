package org.stellar.anchor.sep38

import org.stellar.anchor.event.EventPublishService
import org.stellar.anchor.event.models.AnchorEvent

class MockEventService : EventPublishService {
  override fun publish(event: AnchorEvent?) {
    TODO("Not implemented! Use it with mockk in your tests")
  }
}
