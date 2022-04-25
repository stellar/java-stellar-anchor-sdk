package org.stellar.anchor.platform.paymentobserver

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.config.CirclePaymentObserverConfig
import org.stellar.anchor.exception.AnchorException
import org.stellar.anchor.exception.BadRequestException
import org.stellar.anchor.exception.UnprocessableEntityException

class CirclePaymentObserverServiceTest {
  @MockK private lateinit var httpClient: OkHttpClient
  @MockK private lateinit var circlePaymentObserverConfig: CirclePaymentObserverConfig
  @MockK private lateinit var circlePaymentObserverService: CirclePaymentObserverService
  private lateinit var server: MockWebServer

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true)

    every { circlePaymentObserverConfig.stellarNetwork } returns "TESTNET"
    circlePaymentObserverService =
      CirclePaymentObserverService(httpClient, circlePaymentObserverConfig)

    server = MockWebServer()
    server.start()
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()

    server.shutdown()
  }

  @Test
  fun test_handleCircleNotification_ignoreUnsupportedType() {
    // Empty type is ignored
    var unsupportedNotification = mapOf("foo" to "bar")
    var ex: UnprocessableEntityException = assertThrows {
      circlePaymentObserverService.handleCircleNotification(unsupportedNotification)
    }
    assertEquals("Not handling notification of unsupported type \"\".", ex.message)
    assertInstanceOf(UnprocessableEntityException::class.java, ex)

    // Unsupported type is ignored
    unsupportedNotification = mapOf("Type" to "ABC")
    ex =
      assertThrows {
        circlePaymentObserverService.handleCircleNotification(unsupportedNotification)
      }
    assertEquals("Not handling notification of unsupported type \"ABC\".", ex.message)
    assertInstanceOf(UnprocessableEntityException::class.java, ex)
  }

  @Test
  fun test_handleCircleNotification_handleSubscriptionConfirmationNotification() {
    // missing subscribeUrl
    var subConfirmationNotification = mapOf("Type" to "SubscriptionConfirmation")
    var ex: BadRequestException = assertThrows {
      circlePaymentObserverService.handleCircleNotification(subConfirmationNotification)
    }
    assertEquals(
      "Notification body of type SubscriptionConfirmation is missing subscription URL.",
      ex.message
    )
    assertInstanceOf(BadRequestException::class.java, ex)

    // Test IOException
    every { httpClient.newCall(any()) } throws IOException("Some random IO error!")
    val serverUrl = server.url("").toString()
    subConfirmationNotification =
      mapOf("Type" to "SubscriptionConfirmation", "SubscribeURL" to serverUrl)
    ex =
      assertThrows {
        circlePaymentObserverService.handleCircleNotification(subConfirmationNotification)
      }
    assertEquals("Failed to call \"SubscribeURL\" endpoint.", ex.message)
    assertInstanceOf(BadRequestException::class.java, ex)

    // Failing http request
    val newHttpClient = OkHttpClient.Builder().build()
    circlePaymentObserverService =
      CirclePaymentObserverService(newHttpClient, circlePaymentObserverConfig)

    val badRequestResponse =
      MockResponse()
        .addHeader("Content-Type", "application/json")
        .setResponseCode(400)
        .setBody("""{ "error": "Something went wrong with your request." }""")
    server.enqueue(badRequestResponse)

    ex =
      assertThrows {
        circlePaymentObserverService.handleCircleNotification(subConfirmationNotification)
      }
    assertEquals("Calling the \"SubscribeURL\" endpoint didn't succeed.", ex.message)
    assertInstanceOf(BadRequestException::class.java, ex)

    var request = server.takeRequest()
    assertEquals("GET", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertEquals(serverUrl, request.requestUrl.toString())
    assertNotNull(request.body.readUtf8())

    // Success
    val successResponse =
      MockResponse()
        .addHeader("Content-Type", "application/json")
        .setResponseCode(200)
        .setBody("""{ "success": "ok" }""")
    server.enqueue(successResponse)
    assertDoesNotThrow {
      circlePaymentObserverService.handleCircleNotification(subConfirmationNotification)
    }

    request = server.takeRequest()
    assertEquals("GET", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertEquals(serverUrl, request.requestUrl.toString())
    assertNotNull(request.body.readUtf8())
  }

  @Test
  fun test_handleCircleNotification_handleTransferNotification() {
    // missing Message
    var subConfirmationNotification = mapOf("Type" to "Notification")
    var ex: AnchorException = assertThrows {
      circlePaymentObserverService.handleCircleNotification(subConfirmationNotification)
    }
    assertEquals("Notification body of type Notification is missing a message.", ex.message)
    assertInstanceOf(BadRequestException::class.java, ex)

    // missing Message.transfer
    subConfirmationNotification = mapOf("Type" to "Notification", "Message" to "{}")
    ex =
      assertThrows {
        circlePaymentObserverService.handleCircleNotification(subConfirmationNotification)
      }
    assertEquals("Missing \"transfer\" value in notification of type \"transfers\".", ex.message)
    assertInstanceOf(BadRequestException::class.java, ex)

    // Not a complete transfer
    var messageJson = """{ "transfer": {} }"""
    subConfirmationNotification = mapOf("Type" to "Notification", "Message" to messageJson)
    ex =
      assertThrows {
        circlePaymentObserverService.handleCircleNotification(subConfirmationNotification)
      }
    assertEquals("Not a complete transfer.", ex.message)
    assertInstanceOf(UnprocessableEntityException::class.java, ex)

    // Incomplete transfer
    messageJson = """{ "transfer": { "status": "pending" } }"""
    subConfirmationNotification = mapOf("Type" to "Notification", "Message" to messageJson)
    ex =
      assertThrows {
        circlePaymentObserverService.handleCircleNotification(subConfirmationNotification)
      }
    assertEquals("Not a complete transfer.", ex.message)
    assertInstanceOf(UnprocessableEntityException::class.java, ex)

    // Neither source nor destination are Stellar accounts
    messageJson =
      """{ 
      "transfer": {
        "status": "complete",
        "source": {
          "type": "wallet",
          "id": "1"
        },
        "destination": {
          "type": "wallet",
          "id": "2"
        }
      }
    }"""
        .trimIndent()
        .trimMargin()
    subConfirmationNotification = mapOf("Type" to "Notification", "Message" to messageJson)
    ex =
      assertThrows {
        circlePaymentObserverService.handleCircleNotification(subConfirmationNotification)
      }
    assertEquals("Neither source nor destination are Stellar accounts.", ex.message)
    assertInstanceOf(UnprocessableEntityException::class.java, ex)

    // Neither source nor destination are Stellar accounts
    messageJson =
      """{ 
      "transfer": {
        "status": "complete",
        "source": {
          "type": "blockchain",
          "chain": "XLM"
        },
        "destination": {
          "type": "wallet",
          "id": "1000223064"
        },
        "amount": {
          "amount": "1.50",
          "currency": "ETH"
        }
      }
    }"""
        .trimIndent()
        .trimMargin()
    subConfirmationNotification = mapOf("Type" to "Notification", "Message" to messageJson)
    ex =
      assertThrows {
        circlePaymentObserverService.handleCircleNotification(subConfirmationNotification)
      }
    assertEquals("The only supported Circle currency is USDC.", ex.message)
    assertInstanceOf(UnprocessableEntityException::class.java, ex)
  }
}
