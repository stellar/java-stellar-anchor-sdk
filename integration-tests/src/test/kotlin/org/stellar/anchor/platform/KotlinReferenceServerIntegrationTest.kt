package org.stellar.anchor.platform

import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.skyscreamer.jsonassert.JSONAssert
import org.stellar.anchor.api.callback.SendEventRequest
import org.stellar.anchor.util.StringHelper.json
import org.stellar.reference.client.AnchorReferenceServerClient

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KotlinReferenceServerIntegrationTest {
  private val testProfileRunner =
    TestProfileExecutor(
      TestConfig("default").also {
        it.env[RUN_DOCKER] = "false"
        it.env[RUN_ALL_SERVERS] = "false"
        it.env[RUN_KOTLIN_REFERENCE_SERVER] = "true"
      }
    )

  @BeforeAll
  fun setup() {
    println("Running KotlinReferenceServerIntegrationTest")
    testProfileRunner.start()
  }

  @AfterAll
  fun destroy() {
    testProfileRunner.shutdown()
  }

  @Test
  fun `test if the reference server records the events sent by sendEvent() method`() {
    val client = AnchorReferenceServerClient(Url("http://localhost:8091"))
    val sendEventRequest1 = gson.fromJson(sendEventRequestJson, SendEventRequest::class.java)
    val sendEventRequest2 = gson.fromJson(sendEventRequestJson2, SendEventRequest::class.java)
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
      JSONAssert.assertEquals(json(latestEvent), json(sendEventRequest2), true)
      // check if there are totally two events recorded
      assertEquals(client.getEvents().size, 2)
      JSONAssert.assertEquals(json(client.getEvents()[0]), json(sendEventRequest1), true)
      JSONAssert.assertEquals(json(client.getEvents()[1]), json(sendEventRequest2), true)
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
