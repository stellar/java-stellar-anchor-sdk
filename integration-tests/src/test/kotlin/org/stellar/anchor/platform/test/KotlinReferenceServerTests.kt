package org.stellar.anchor.platform.test

import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.event.EventService
import org.stellar.anchor.platform.event.DefaultEventService
import org.stellar.anchor.util.GsonUtils

class KotlinReferenceServerTests {
  fun testEvents() {
    val eventService = DefaultEventService(EventProcessingServerTests.eventConfig)
    val session = eventService.createSession("testOk", EventService.EventQueue.TRANSACTION)
    val quoteEvent = GsonUtils.getInstance().fromJson(testQuoteEvent, AnchorEvent::class.java)

    session.publish(quoteEvent)
  }
  fun testAll() {
    println("Performing kotlin reference server tests...")
    testEvents()
  }
}
