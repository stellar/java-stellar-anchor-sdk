package org.stellar.anchor.platform.integrationtest

import io.ktor.http.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.stellar.anchor.api.callback.SendEventRequest
import org.stellar.anchor.platform.AbstractIntegrationTests
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.platform.gson
import org.stellar.anchor.util.StringHelper.json
import org.stellar.reference.client.AnchorReferenceServerClient

class ReferenceServerTests : AbstractIntegrationTests(TestConfig(testProfileName = "default")) {
  @Test
  fun `test if the reference server records the events sent by sendEvent() method`() {
    val client = AnchorReferenceServerClient(Url("http://localhost:8091"))
    val sendEventRequest1 = gson.fromJson(sendEventRequestJson, SendEventRequest::class.java)
    val sendEventRequest2 = gson.fromJson(sendEventRequestJson2, SendEventRequest::class.java)
    sendEventRequest1.id = UUID.randomUUID().toString()
    sendEventRequest2.id = UUID.randomUUID().toString()
    runBlocking {
      // Send event1
      client.sendEvent(sendEventRequest1)
      var latestEvent = client.getLatestEvent()
      Assertions.assertNotNull(latestEvent)
      JSONAssert.assertEquals(json(latestEvent), json(sendEventRequest1), true)
      // send event2
      client.sendEvent(sendEventRequest2)
      latestEvent = client.getLatestEvent()
      Assertions.assertNotNull(latestEvent)
      // check if these events are recorded
      client
        .getEvents()
        .stream()
        .filter { it.id == sendEventRequest1.id }
        .findFirst()
        .get()
        .apply { JSONAssert.assertEquals(json(this), json(sendEventRequest1), true) }
      client
        .getEvents()
        .stream()
        .filter { it.id == sendEventRequest2.id }
        .findFirst()
        .get()
        .apply { JSONAssert.assertEquals(json(this), json(sendEventRequest2), true) }
    }
  }

  companion object {
    val sendEventRequestJson =
      """
      {
          "timestamp": "2011-10-05T14:48:00.000Z",
          "id": "2a419880-0dde-4821-90cb-f3bfcb671ea3",
          "type": "transaction_created",
          "payload": {
            "transaction": {
              "amount_in": {
                "amount": "10.0",
                "asset": "USDC"
              }
            }
          }
      }
    """
        .trimIndent()

    val sendEventRequestJson2 =
      """
{
  "id": "2a419880-0dde-4821-90cb-f3bfcb671ea3",
  "timestamp": "2011-10-05T14:48:00.000Z",
  "type": "quote_created",
  "payload": {
    "quote": {
      "sell_amount": "10.0",
      "buy_amount": "1"
    }
  }
}
      """
        .trimIndent()
  }
}
