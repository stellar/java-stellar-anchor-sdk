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
import org.stellar.anchor.horizon.Horizon
import org.stellar.anchor.paymentservice.circle.model.CircleBalance
import org.stellar.anchor.paymentservice.circle.model.CircleTransfer
import org.stellar.sdk.*
import org.stellar.sdk.responses.Page
import org.stellar.sdk.responses.TransactionResponse
import org.stellar.sdk.responses.operations.OperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse

class CirclePaymentObserverServiceTest {
  @MockK private lateinit var httpClient: OkHttpClient
  @MockK private lateinit var horizon: Horizon
  @MockK private lateinit var circlePaymentObserverConfig: CirclePaymentObserverConfig
  @MockK private lateinit var circlePaymentObserverService: CirclePaymentObserverService
  private lateinit var server: MockWebServer

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true)

    every { circlePaymentObserverConfig.stellarNetwork } returns "TESTNET"
    circlePaymentObserverService =
      CirclePaymentObserverService(httpClient, circlePaymentObserverConfig, horizon)

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
      CirclePaymentObserverService(newHttpClient, circlePaymentObserverConfig, horizon)

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

  @Test
  fun test_fetchObservedPayment() {
    // Mock horizon call
    val mockedServer = mockk<Server>()
    val mockedOpResponsePage = mockk<Page<OperationResponse>>()
    every { horizon.server } returns mockedServer
    every {
      mockedServer
        .payments()
        .forTransaction(any())
        .limit(any())
        .includeTransactions(any())
        .execute()
    } returns mockedOpResponsePage
    circlePaymentObserverService =
      CirclePaymentObserverService(httpClient, circlePaymentObserverConfig, horizon)

    // if the response is empty, returns null
    val circleTransfer = CircleTransfer()
    circleTransfer.transactionHash =
      "b9d0b2292c4e09e8eb22d036171491e87b8d2086bf8b265874c8d182cb9c9020"
    every { mockedOpResponsePage.records } returns ArrayList()
    var observedPayment = circlePaymentObserverService.fetchObservedPayment(circleTransfer)
    verify(exactly = 1) { mockedOpResponsePage.records }
    assertNull(observedPayment)

    // if the response operation is not successful, returns null
    val mockedOpResponse = mockk<OperationResponse>()
    every { mockedOpResponse.isTransactionSuccessful } returns false
    every { mockedOpResponsePage.records } returns arrayListOf(mockedOpResponse)
    observedPayment = circlePaymentObserverService.fetchObservedPayment(circleTransfer)
    verify(exactly = 2) { mockedOpResponsePage.records }
    verify(exactly = 1) { mockedOpResponse.isTransactionSuccessful }
    assertNull(observedPayment)

    // if the response does not contain any payment, returns null
    every { mockedOpResponse.isTransactionSuccessful } returns true
    every { mockedOpResponse.type } returns "create_account"
    every { mockedOpResponsePage.records } returns arrayListOf(mockedOpResponse)
    observedPayment = circlePaymentObserverService.fetchObservedPayment(circleTransfer)
    verify(exactly = 3) { mockedOpResponsePage.records }
    verify(exactly = 2) { mockedOpResponse.isTransactionSuccessful }
    verify(exactly = 1) { mockedOpResponse.type }
    assertNull(observedPayment)

    // if asset type is not AssetTypeCreditAlphaNum, returns null
    circleTransfer.amount = CircleBalance("USD", "1.234")

    val mockedTransaction = mockk<TransactionResponse>()
    every { mockedTransaction.sourceAccount } returns
      "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S"
    every { mockedTransaction.envelopeXdr } returns "my_envelope_xdr"
    every { mockedTransaction.memo } returns Memo.text("my_text_memo")

    val usdcAsset =
      AssetTypeCreditAlphaNum4("USDC", "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
    val mockedPaymentOpResponse = mockk<PaymentOperationResponse>()
    every { mockedPaymentOpResponse.transaction.get() } returns mockedTransaction
    every { mockedPaymentOpResponse.isTransactionSuccessful } returns true
    every { mockedPaymentOpResponse.type } returns "payment"
    every { mockedPaymentOpResponse.asset } returns usdcAsset
    every { mockedPaymentOpResponse.sourceAccount } returns
      "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S"
    every { mockedPaymentOpResponse.id } returns 755914248193
    every { mockedPaymentOpResponse.to } returns
      "GAYF33NNNMI2Z6VNRFXQ64D4E4SF77PM46NW3ZUZEEU5X7FCHAZCMHKU"
    every { mockedPaymentOpResponse.from } returns
      "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S"
    every { mockedPaymentOpResponse.amount } returns "1.2340000"
    every { mockedPaymentOpResponse.createdAt } returns "2022-03-16T10:02:39Z"
    every { mockedPaymentOpResponse.transactionHash } returns
      "b9d0b2292c4e09e8eb22d036171491e87b8d2086bf8b265874c8d182cb9c9020"
    every { mockedOpResponsePage.records } returns arrayListOf(mockedPaymentOpResponse)
    observedPayment = circlePaymentObserverService.fetchObservedPayment(circleTransfer)

    val wantObservedPayment =
      ObservedPayment.builder()
        .id("755914248193")
        .type(ObservedPayment.Type.PAYMENT)
        .from("GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S")
        .to("GAYF33NNNMI2Z6VNRFXQ64D4E4SF77PM46NW3ZUZEEU5X7FCHAZCMHKU")
        .amount("1.2340000")
        .assetType("credit_alphanum4")
        .assetCode("USDC")
        .assetIssuer("GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
        .assetName("USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
        .sourceAccount("GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S")
        .createdAt("2022-03-16T10:02:39Z")
        .transactionHash("b9d0b2292c4e09e8eb22d036171491e87b8d2086bf8b265874c8d182cb9c9020")
        .transactionMemo("my_text_memo")
        .transactionMemoType("text")
        .transactionEnvelope("my_envelope_xdr")
        .build()
    assertEquals(wantObservedPayment, observedPayment)
  }
}
