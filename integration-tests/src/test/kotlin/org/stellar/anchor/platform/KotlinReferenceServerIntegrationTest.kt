package org.stellar.anchor.platform

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.stellar.anchor.api.callback.SendEventRequest
import org.stellar.anchor.apiclient.CallbackApiClient
import org.stellar.anchor.auth.AuthHelper

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KotlinReferenceServerIntegrationTest {
  private val testProfileRunner =
    TestProfileExecutor(
      TestConfig("sep24").also {
        it.env["run_all_servers"] = "false"
        it.env["run_kotlin_reference_server"] = "true"
        it.env["run_docker"] = "false"
      }
    )

  @BeforeAll
  fun setup() {
    //    testProfileRunner.start()
  }

  @AfterAll
  fun destroy() {
    //    testProfileRunner.shutdown()
  }

  @Test
  fun `test if the reference server records the events sent by sendEvent() method`() {
    val callbackApiClient = CallbackApiClient(AuthHelper.forNone(), "http://localhost:8091")
    val response =
      callbackApiClient.sendEvent(gson.fromJson(sendEventRequestJson, SendEventRequest::class.java))
    println(response)
  }

  companion object {
    val sendEventRequestJson =
      """
      {
          "timestamp": 1000000,
          "payload": {
              "type": "TRANSACTION_CREATED",
              "id": 100,
              "sep": 24,
              "transaction": {}
          }
      }
    """
        .trimIndent()
  }
}
