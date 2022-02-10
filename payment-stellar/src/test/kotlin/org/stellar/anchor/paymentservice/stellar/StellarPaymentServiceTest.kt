package org.stellar.anchor.paymentservice.stellar

import java.io.IOException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.paymentservice.PaymentService
import org.stellar.sdk.KeyPair

class StellarPaymentServiceTest {
  private lateinit var server: MockWebServer
  private lateinit var service: PaymentService

  @BeforeEach
  @Throws(IOException::class)
  fun setUp() {
    server = MockWebServer()
    server.start()
    System.out.println(server.url("").toString())
    service =
      StellarPaymentService(
        server.url("").toString(),
        KeyPair.random().secretSeed.toString(),
        100,
        org.stellar.sdk.Network.TESTNET,
        15
      )
  }

  @AfterEach
  @Throws(IOException::class)
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun testSuccessfulPing() {
    val response = MockResponse().setResponseCode(200)
    server.enqueue(response)

    Assertions.assertDoesNotThrow { service.ping().block() }
    server.takeRequest()
  }
}
