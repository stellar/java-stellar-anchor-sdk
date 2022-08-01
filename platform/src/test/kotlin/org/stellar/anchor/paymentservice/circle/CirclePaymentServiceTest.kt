package org.stellar.anchor.paymentservice.circle

import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.lang.reflect.Method
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeFormatter
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.skyscreamer.jsonassert.JSONAssert
import org.stellar.anchor.api.exception.HttpException
import org.stellar.anchor.config.CircleConfig
import org.stellar.anchor.horizon.Horizon
import org.stellar.anchor.platform.payment.common.*
import org.stellar.anchor.platform.payment.config.CirclePaymentConfig
import org.stellar.anchor.platform.payment.observer.circle.CirclePaymentService
import org.stellar.anchor.platform.payment.observer.circle.model.CircleBlockchainAddress
import org.stellar.anchor.platform.payment.observer.circle.model.CircleWallet
import org.stellar.anchor.platform.payment.observer.circle.model.CircleWireDepositInstructions
import org.stellar.anchor.platform.payment.observer.circle.model.response.CircleDetailResponse
import org.stellar.anchor.platform.payment.observer.circle.model.response.CircleListResponse
import org.stellar.anchor.platform.payment.observer.circle.util.CircleAsset
import org.stellar.anchor.util.FileUtil
import org.stellar.anchor.util.GsonUtils
import org.stellar.sdk.Network
import org.stellar.sdk.Server
import org.stellar.sdk.responses.Page
import org.stellar.sdk.responses.operations.OperationResponse
import reactor.core.publisher.Mono
import shadow.com.google.common.reflect.TypeToken

class CirclePaymentServiceTest {
  companion object {
    val mockWalletToWalletTransferJson: String =
      FileUtil.getResourceFileAsString("mock_wallet_to_wallet_transfer.json")

    val mockStellarToWalletTransferJson: String =
      FileUtil.getResourceFileAsString("mock_stellar_to_wallet_transfer.json")

    val mockWalletToStellarTransferJson: String =
      FileUtil.getResourceFileAsString("mock_wallet_to_stellar_transfer.json")

    val mockWalletToWirePayoutJson: String =
      FileUtil.getResourceFileAsString("mock_wallet_to_wire_payout.json")

    val mockWireToWalletPaymentJson: String =
      FileUtil.getResourceFileAsString("mock_wire_to_wallet_payment.json")

    val mockStellarPaymentResponsePageBody: String =
      FileUtil.getResourceFileAsString("mock_stellar_payment_response_page_body.json")

    val mockStellarPathPaymentResponsePageBody: String =
      FileUtil.getResourceFileAsString("mock_stellar_path_payment_response_page_body.json")

    val mockGetListOfAddressesBody: String =
      FileUtil.getResourceFileAsString("mock_get_list_of_addresses_body.json")

    val mockGetWireDepositInstructionsBody: String =
      FileUtil.getResourceFileAsString("mock_get_wire_deposit_instructions_body.json")

    val mockAddressJson: String = FileUtil.getResourceFileAsString("mock_address.json")
  }

  private fun instantFromString(dateStr: String): Instant {
    return DateTimeFormatter.ISO_INSTANT.parse(dateStr, Instant::from)
  }

  private lateinit var server: MockWebServer
  private lateinit var service: PaymentService
  private val merchantAccount =
    Account(
      PaymentNetwork.CIRCLE,
      "1000066041",
      Account.Capabilities(PaymentNetwork.CIRCLE, PaymentNetwork.STELLAR, PaymentNetwork.BANK_WIRE)
    )

  private fun getDistAccountIdMockResponse(masterWalletId: String = "1000066041"): MockResponse {
    return MockResponse()
      .addHeader("Content-Type", "application/json")
      .setBody(
        """{
          "data":{
            "payments":{
              "masterWalletId":"$masterWalletId"
            }
          }
        }"""
      )
  }

  @BeforeEach
  @Throws(IOException::class)
  fun setUp() {
    server = MockWebServer()
    server.start()

    val circlePaymentConfig = mockk<CirclePaymentConfig>()
    every { circlePaymentConfig.isEnabled } returns true
    every { circlePaymentConfig.name } returns "TestCircle"

    val circleConfig = mockk<CircleConfig>()
    every { circleConfig.circleUrl } returns server.url("").toString()
    every { circleConfig.apiKey } returns "<secret-key>"

    val horizon = mockk<Horizon>()
    every { horizon.horizonUrl } returns server.url("").toString()
    every { horizon.stellarNetworkPassphrase } returns "Test SDF Network ; September 2015"
    every { horizon.server } returns Server(server.url("").toString())

    service = CirclePaymentService(circlePaymentConfig, circleConfig, horizon)
  }

  @AfterEach
  @Throws(IOException::class)
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun test_ping() {
    val response =
      MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody("{\"message\": \"pong\"}")
    server.enqueue(response)

    assertDoesNotThrow { service.ping().block() }
    val request = server.takeRequest()
    assertEquals("GET", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertThat(request.path, CoreMatchers.endsWith("/ping"))
  }

  @Test
  fun test_getDistributionAccountAddress() {
    server.enqueue(getDistAccountIdMockResponse())

    var masterWalletId: String? = null
    assertDoesNotThrow { masterWalletId = service.distributionAccountAddress.block() }
    assertEquals("1000066041", masterWalletId)

    val request = server.takeRequest()
    assertEquals("GET", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", request.headers["Authorization"])
    assertThat(request.path, CoreMatchers.endsWith("/v1/configuration"))

    // check if cached version doesn't freeze the thread
    assertDoesNotThrow { masterWalletId = service.distributionAccountAddress.block() }
    assertEquals("1000066041", masterWalletId)
  }

  @Test
  fun test_private_getMerchantAccountUnsettledBalances() {
    val response =
      MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody(
          """{
                        "data":{
                            "available":[],
                            "unsettled": [
                                {
                                    "amount":"100.00",
                                    "currency":"USD"
                                }
                            ]
                        }
                    }"""
        )
    server.enqueue(response)

    // Let's use reflection to access the private method
    val getMerchantAccountUnsettledBalancesMethod: Method =
      CirclePaymentService::class.java.getDeclaredMethod("getMerchantAccountUnsettledBalances")
    assert(getMerchantAccountUnsettledBalancesMethod.trySetAccessible())
    @Suppress("UNCHECKED_CAST")
    val unsettledBalancesMono =
      (getMerchantAccountUnsettledBalancesMethod.invoke(service) as Mono<List<Balance>>)

    var unsettledBalances: List<Balance>? = null
    assertDoesNotThrow { unsettledBalances = unsettledBalancesMono.block() }
    assertEquals(1, unsettledBalances?.size)
    assertEquals("100.00", unsettledBalances!![0].amount)
    assertEquals("circle:USD", unsettledBalances!![0].currencyName)

    val request = server.takeRequest()
    assertEquals("GET", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", request.headers["Authorization"])
    assertThat(request.path, CoreMatchers.endsWith("/v1/businessAccount/balances"))
  }

  @Test
  fun test_private_getCircleWallet() {
    val response =
      MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody(
          """{
                        "data":{
                            "walletId":"1000223064",
                            "entityId":"2f47c999-9022-4939-acea-dc3afa9ccbaf",
                            "type":"end_user_wallet",
                            "description":"Treasury Wallet",
                            "balances":[
                                {
                                    "amount":"123.45",
                                    "currency":"USD"
                                }
                            ]
                        }
                    }"""
        )
    server.enqueue(response)

    // Let's use reflection to access the private method
    val getCircleWalletMethod: Method =
      CirclePaymentService::class.java.getDeclaredMethod("getCircleWallet", String::class.java)
    assert(getCircleWalletMethod.trySetAccessible())

    var circleWallet: CircleWallet? = null
    assertDoesNotThrow {
      @Suppress("UNCHECKED_CAST")
      circleWallet =
        (getCircleWalletMethod.invoke(service, "1000223064") as Mono<CircleWallet>).block()
    }
    assertEquals("1000223064", circleWallet?.walletId)
    assertEquals("2f47c999-9022-4939-acea-dc3afa9ccbaf", circleWallet?.entityId)
    assertEquals("end_user_wallet", circleWallet?.type)
    assertEquals("Treasury Wallet", circleWallet?.description)
    assertEquals(1, circleWallet?.balances?.size)
    assertEquals("123.45", circleWallet!!.balances[0].amount)
    assertEquals("USD", circleWallet!!.balances[0].currency)

    val request = server.takeRequest()
    assertEquals("GET", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", request.headers["Authorization"])
    assertTrue(request.path!!.endsWith("/v1/wallets/1000223064"))
  }

  @Test
  fun test_getAccount_isNotMainAccount() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/configuration" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{
                                    "data":{
                                        "payments":{
                                            "masterWalletId":"1234"
                                        }
                                    }
                            }"""
                )
            "/v1/wallets/1000223064" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{
                                    "data":{
                                        "walletId":"1000223064",
                                        "entityId":"2f47c999-9022-4939-acea-dc3afa9ccbaf",
                                        "type":"end_user_wallet",
                                        "description":"Treasury Wallet",
                                        "balances":[
                                            {
                                                "amount":"29472389929.00",
                                                "currency":"USD"
                                            }
                                        ]
                                    }
                                }"""
                )
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    var account: Account? = null
    assertDoesNotThrow { account = service.getAccount("1000223064").block() }
    assertEquals("1000223064", account?.id)
    assertEquals(PaymentNetwork.CIRCLE, account?.paymentNetwork)
    assertEquals(CircleWallet.defaultCapabilities(), account?.capabilities)
    assertEquals("Treasury Wallet", account?.idTag)
    assertEquals(1, account?.balances?.size)
    assertEquals("29472389929.00", account!!.balances!![0].amount)
    assertEquals("circle:USD", account!!.balances!![0].currencyName)
    assertEquals(0, account?.unsettledBalances?.size)

    assertEquals(2, server.requestCount)
    val allRequests = arrayOf(server.takeRequest(), server.takeRequest())

    val validateSecretKeyRequest =
      allRequests.find { request -> request.path!! == "/v1/configuration" }!!
    assertEquals("GET", validateSecretKeyRequest.method)
    assertEquals("application/json", validateSecretKeyRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", validateSecretKeyRequest.headers["Authorization"])

    val getAccountRequest =
      allRequests.find { request -> request.path!! == "/v1/wallets/1000223064" }!!
    assertEquals("GET", getAccountRequest.method)
    assertEquals("application/json", getAccountRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", getAccountRequest.headers["Authorization"])
  }

  @Test
  fun test_getAccount_isMainAccount() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/configuration" -> return getDistAccountIdMockResponse()
            "/v1/businessAccount/balances" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{
                                    "data":{
                                        "available":[],
                                        "unsettled": [
                                            {
                                                "amount":"100.00",
                                                "currency":"USD"
                                            }
                                        ]
                                    }
                                }"""
                )
            "/v1/wallets/1000066041" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{
                                    "data":{
                                        "walletId":"1000066041",
                                        "entityId":"2f47c999-9022-4939-acea-dc3afa9ccbaf",
                                        "type":"merchant",
                                        "balances":[
                                            {
                                                "amount":"29472389929.00",
                                                "currency":"USD"
                                            }
                                        ]
                                    }
                                }"""
                )
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    var account: Account? = null
    assertDoesNotThrow { account = service.getAccount("1000066041").block() }
    assertEquals("1000066041", account?.id)
    assertEquals(PaymentNetwork.CIRCLE, account?.paymentNetwork)
    assertEquals(
      Account.Capabilities(PaymentNetwork.CIRCLE, PaymentNetwork.STELLAR, PaymentNetwork.BANK_WIRE),
      account?.capabilities
    )
    assertNull(account?.idTag)
    assertEquals(1, account?.balances?.size)
    assertEquals("29472389929.00", account!!.balances!![0].amount)
    assertEquals("circle:USD", account!!.balances!![0].currencyName)
    assertEquals(1, account?.unsettledBalances?.size)
    assertEquals("100.00", account!!.unsettledBalances!![0].amount)
    assertEquals("circle:USD", account!!.unsettledBalances!![0].currencyName)

    assertEquals(3, server.requestCount)

    val validateSecretKeyRequest = server.takeRequest()
    assertEquals("GET", validateSecretKeyRequest.method)
    assertEquals("application/json", validateSecretKeyRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", validateSecretKeyRequest.headers["Authorization"])
    assertTrue(validateSecretKeyRequest.path!!.endsWith("/v1/configuration"))

    val parallelRequests = arrayOf(server.takeRequest(), server.takeRequest())

    val mainAccountRequest =
      parallelRequests.find { request -> request.path!! == "/v1/businessAccount/balances" }!!
    assertEquals("GET", mainAccountRequest.method)
    assertEquals("application/json", mainAccountRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", mainAccountRequest.headers["Authorization"])

    val getAccountRequest =
      parallelRequests.find { request -> request.path!! == "/v1/wallets/1000066041" }!!
    assertEquals("GET", getAccountRequest.method)
    assertEquals("application/json", getAccountRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", getAccountRequest.headers["Authorization"])
  }

  @Test
  fun test_createAccount() {
    val response =
      MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody(
          """{
                        "data":{
                            "walletId":"1000223064",
                            "entityId":"2f47c999-9022-4939-acea-dc3afa9ccbaf",
                            "type":"end_user_wallet",
                            "description":"Foo bar",
                            "balances":[
                                {
                                    "amount":"123.45",
                                    "currency":"USD"
                                }
                            ]
                        }
                    }"""
        )
    server.enqueue(response)

    var account: Account? = null
    assertDoesNotThrow { account = service.createAccount("Foo bar").block() }
    assertEquals("1000223064", account?.id)
    assertEquals(PaymentNetwork.CIRCLE, account?.paymentNetwork)
    assertEquals(CircleWallet.defaultCapabilities(), account?.capabilities)
    assertEquals("Foo bar", account?.idTag)
    assertEquals(1, account?.balances?.size)
    assertEquals("123.45", account!!.balances!![0].amount)
    assertEquals("circle:USD", account!!.balances!![0].currencyName)
    assertEquals(0, account?.unsettledBalances?.size)

    val request = server.takeRequest()
    val requestBody = request.body.readUtf8()
    assertEquals("POST", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", request.headers["Authorization"])
    assertThat(request.path, CoreMatchers.endsWith("/v1/wallets"))
    assertThat(requestBody, CoreMatchers.containsString("\"description\":\"Foo bar\""))
    assertThat(requestBody, CoreMatchers.containsString("\"idempotencyKey\":"))
  }

  @Test
  fun test_sendPayment_parameterValidation() {
    // invalid source account network
    var ex =
      assertThrows<HttpException> {
        service
          .sendPayment(
            Account(PaymentNetwork.STELLAR, "123", Account.Capabilities()),
            Account(PaymentNetwork.CIRCLE, "123", Account.Capabilities()),
            "",
            BigDecimal(0)
          )
          .block()
      }
    assertEquals(
      HttpException(400, "the only supported network for the source account is circle"),
      ex
    )

    // missing beneficiary email when destination is a wire bank account
    ex =
      assertThrows {
        service
          .sendPayment(
            Account(PaymentNetwork.CIRCLE, "123", Account.Capabilities()),
            Account(PaymentNetwork.BANK_WIRE, "123", "invalidEmail", Account.Capabilities()),
            "",
            BigDecimal(0)
          )
          .block()
      }
    assertEquals(
      HttpException(
        400,
        "for bank transfers, please provide a valid beneficiary email address in the destination idTag"
      ),
      ex
    )

    // invalid currency name schema
    ex =
      assertThrows {
        service
          .sendPayment(
            Account(PaymentNetwork.CIRCLE, "123", Account.Capabilities()),
            Account(PaymentNetwork.CIRCLE, "456", Account.Capabilities()),
            "invalidSchema:USD",
            BigDecimal(0)
          )
          .block()
      }
    assertEquals(
      HttpException(400, "the currency to be sent must contain the destination network schema"),
      ex
    )

    // invalid currency name
    ex =
      assertThrows {
        service
          .sendPayment(
            Account(PaymentNetwork.CIRCLE, "123", Account.Capabilities()),
            Account(PaymentNetwork.BANK_WIRE, "456", "email@test.com", Account.Capabilities()),
            "iso4217:ABC",
            BigDecimal(0)
          )
          .block()
      }
    val wantException =
      HttpException(
        400,
        String.format(
          "the only supported currencies are %s, %s and %s.",
          "circle:USD",
          "iso4217:USD",
          CircleAsset.stellarUSDC(Network.TESTNET)
        )
      )
    assertEquals(wantException, ex)
  }

  @Test
  fun test_private_validateSendPaymentInput_supportedCurrencies() {
    // Let's use reflection to access the private method
    val validateSendPaymentInputMethod: Method =
      CirclePaymentService::class.java.getDeclaredMethod(
        "validateSendPaymentInput",
        Account::class.java,
        Account::class.java,
        String::class.java
      )
    assert(validateSendPaymentInputMethod.trySetAccessible())

    // "circle:USD" succeeds
    assertDoesNotThrow {
      @Suppress("UNCHECKED_CAST")
      validateSendPaymentInputMethod.invoke(
        service,
        Account(PaymentNetwork.CIRCLE, "123", Account.Capabilities()),
        Account(PaymentNetwork.CIRCLE, "456", Account.Capabilities()),
        "circle:USD"
      )
    }

    // "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5" succeeds
    assertDoesNotThrow {
      @Suppress("UNCHECKED_CAST")
      validateSendPaymentInputMethod.invoke(
        service,
        Account(PaymentNetwork.CIRCLE, "123", Account.Capabilities()),
        Account(PaymentNetwork.STELLAR, "G...", Account.Capabilities()),
        "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      )
    }

    // "iso4217:USD" succeeds
    assertDoesNotThrow {
      @Suppress("UNCHECKED_CAST")
      validateSendPaymentInputMethod.invoke(
        service,
        Account(PaymentNetwork.CIRCLE, "123", Account.Capabilities()),
        Account(PaymentNetwork.BANK_WIRE, "456", "email@test.com", Account.Capabilities()),
        "iso4217:USD"
      )
    }
  }

  @Test
  fun test_sendPayment_circleToCircle() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/configuration" -> return getDistAccountIdMockResponse()
            "/v1/transfers" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{    
                            "data": {
                                "id":"c58e2613-a808-4075-956c-e576787afb3b",
                                "source":{
                                    "type":"wallet",
                                    "id":"1000066041"
                                },
                                "destination":{
                                    "type":"wallet",
                                    "id":"1000067536"
                                },
                                "amount":{
                                    "amount":"0.91",
                                    "currency":"USD"
                                },
                                "status":"pending",
                                "createDate":"2022-01-01T01:01:01.544Z"
                            }
                        }""".trimIndent()
                )
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    val destination =
      Account(
        PaymentNetwork.CIRCLE,
        "1000067536",
        Account.Capabilities(PaymentNetwork.CIRCLE, PaymentNetwork.STELLAR)
      )
    var payment: Payment? = null
    assertDoesNotThrow {
      payment =
        service
          .sendPayment(
            merchantAccount,
            destination,
            CircleAsset.circleUSD(),
            BigDecimal.valueOf(0.91)
          )
          .block()
    }

    assertEquals("c58e2613-a808-4075-956c-e576787afb3b", payment?.id)
    assertEquals(merchantAccount, payment?.sourceAccount)
    assertEquals(
      Account(PaymentNetwork.CIRCLE, "1000067536", CircleWallet.defaultCapabilities()),
      payment?.destinationAccount
    )
    assertEquals(Balance("0.91", "circle:USD"), payment?.balance)
    assertEquals(Payment.Status.PENDING, payment?.status)
    assertNull(payment?.errorCode)

    val wantDate = instantFromString("2022-01-01T01:01:01.544Z")
    assertEquals(wantDate, payment?.createdAt)
    assertEquals(wantDate, payment?.updatedAt)

    val wantOriginalResponse =
      hashMapOf<String, Any>(
        "id" to "c58e2613-a808-4075-956c-e576787afb3b",
        "source" to hashMapOf<String, Any>("id" to "1000066041", "type" to "wallet"),
        "destination" to
          hashMapOf<String, Any>(
            "id" to "1000067536",
            "type" to "wallet",
          ),
        "amount" to
          hashMapOf<String, Any>(
            "amount" to "0.91",
            "currency" to "USD",
          ),
        "status" to "pending",
        "createDate" to "2022-01-01T01:01:01.544Z",
      )
    assertEquals(wantOriginalResponse, payment?.originalResponse)

    assertEquals(2, server.requestCount)
    val allRequests = arrayOf(server.takeRequest(), server.takeRequest())

    val validateSecretKeyRequest =
      allRequests.find { request -> request.path!! == "/v1/configuration" }!!
    assertEquals("GET", validateSecretKeyRequest.method)
    assertEquals("application/json", validateSecretKeyRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", validateSecretKeyRequest.headers["Authorization"])

    val postTransferRequest = allRequests.find { request -> request.path!! == "/v1/transfers" }!!
    assertEquals("POST", postTransferRequest.method)
    assertEquals("application/json", postTransferRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", postTransferRequest.headers["Authorization"])
    val gotBody = postTransferRequest.body.readUtf8()
    val wantBody =
      """{
            "source": {
                "type": "wallet",
                "id": "1000066041"
            },
            "destination": {
                "type": "wallet",
                "id": "1000067536"
            },
            "amount": {
                "amount": "0.91",
                "currency": "USD"
            }
        }""".trimIndent()
    JSONAssert.assertEquals(wantBody, gotBody, false)
  }

  @Test
  fun test_sendPayment_circleToStellar() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/configuration" -> return getDistAccountIdMockResponse()
            "/v1/transfers" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{    
                            "data": {
                                "id":"c58e2613-a808-4075-956c-e576787afb3b",
                                "source":{
                                    "type":"wallet",
                                    "id":"1000066041"
                                },
                                "destination":{
                                    "type":"blockchain",
                                    "address":"GBG7VGZFH4TU2GS7WL5LMPYFNP64ZFR23XEGAV7GPEEXKWOR2DKCYPCK",
                                    "addressTag":"test tag",
                                    "chain":"XLM"
                                },
                                "amount":{
                                    "amount":"0.91",
                                    "currency":"USD"
                                },
                                "status":"pending",
                                "createDate":"2022-01-01T01:01:01.544Z"
                            }
                        }""".trimIndent()
                )
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    val source = Account(PaymentNetwork.CIRCLE, "1000066041", Account.Capabilities())
    val destination =
      Account(
        PaymentNetwork.STELLAR,
        "GBG7VGZFH4TU2GS7WL5LMPYFNP64ZFR23XEGAV7GPEEXKWOR2DKCYPCK",
        "test tag",
        Account.Capabilities()
      )
    var payment: Payment? = null
    assertDoesNotThrow {
      payment =
        service
          .sendPayment(
            source,
            destination,
            "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
            BigDecimal.valueOf(0.91)
          )
          .block()
    }

    assertEquals("c58e2613-a808-4075-956c-e576787afb3b", payment?.id)
    assertEquals(merchantAccount, payment?.sourceAccount)
    assertEquals(
      Account(
        PaymentNetwork.STELLAR,
        "GBG7VGZFH4TU2GS7WL5LMPYFNP64ZFR23XEGAV7GPEEXKWOR2DKCYPCK",
        "test tag",
        Account.Capabilities(PaymentNetwork.STELLAR)
      ),
      payment?.destinationAccount
    )
    assertEquals(
      Balance("0.91", "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"),
      payment?.balance
    )
    assertEquals(Payment.Status.PENDING, payment?.status)
    assertNull(payment?.errorCode)

    val wantDate = instantFromString("2022-01-01T01:01:01.544Z")
    assertEquals(wantDate, payment?.createdAt)
    assertEquals(wantDate, payment?.updatedAt)

    val wantOriginalResponse =
      hashMapOf<String, Any>(
        "id" to "c58e2613-a808-4075-956c-e576787afb3b",
        "source" to
          hashMapOf<String, Any>(
            "id" to "1000066041",
            "type" to "wallet",
          ),
        "destination" to
          hashMapOf<String, Any>(
            "address" to "GBG7VGZFH4TU2GS7WL5LMPYFNP64ZFR23XEGAV7GPEEXKWOR2DKCYPCK",
            "addressTag" to "test tag",
            "type" to "blockchain",
            "chain" to "XLM",
          ),
        "amount" to
          hashMapOf<String, Any>(
            "amount" to "0.91",
            "currency" to "USD",
          ),
        "status" to "pending",
        "createDate" to "2022-01-01T01:01:01.544Z",
      )
    assertEquals(wantOriginalResponse, payment?.originalResponse)

    assertEquals(2, server.requestCount)
    val allRequests = arrayOf(server.takeRequest(), server.takeRequest())

    val validateSecretKeyRequest =
      allRequests.find { request -> request.path!! == "/v1/configuration" }!!
    assertEquals("GET", validateSecretKeyRequest.method)
    assertEquals("application/json", validateSecretKeyRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", validateSecretKeyRequest.headers["Authorization"])

    val postTransferRequest = allRequests.find { request -> request.path!! == "/v1/transfers" }!!
    assertEquals("POST", postTransferRequest.method)
    assertEquals("application/json", postTransferRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", postTransferRequest.headers["Authorization"])
    val gotBody = postTransferRequest.body.readUtf8()
    val wantBody =
      """{
            "source": {
                "type": "wallet",
                "id": "1000066041"
            },
            "destination": {
                "type": "blockchain",
                "address": "GBG7VGZFH4TU2GS7WL5LMPYFNP64ZFR23XEGAV7GPEEXKWOR2DKCYPCK",
                "addressTag": "test tag",
                "chain": "XLM"
            },
            "amount": {
                "amount": "0.91",
                "currency": "USD"
            }
        }""".trimIndent()
    JSONAssert.assertEquals(wantBody, gotBody, false)
  }

  @Test
  fun test_sendPayment_circleToWire() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/configuration" -> return getDistAccountIdMockResponse()
            "/v1/payouts" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{    
                                    "data": {
                                        "id":"c58e2613-a808-4075-956c-e576787afb3b",
                                        "amount":{
                                            "amount":"0.91",
                                            "currency":"USD"
                                        },
                                        "fees":{
                                            "amount":"0.09",
                                            "currency":"USD"
                                        },
                                        "status":"pending",
                                        "sourceWalletId":"1000066041",
                                        "destination":{
                                            "type":"wire",
                                            "id":"6c87da10-feb8-484f-822c-2083ed762d25",
                                            "name":"JPMORGAN CHASE BANK, NA ****6789"
                                        },
                                        "createDate":"2021-11-25T15:43:03.477Z",
                                        "updateDate":"2021-11-25T16:10:01.618Z"
                                    }
                            }""".trimIndent()
                )
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    val source = Account(PaymentNetwork.CIRCLE, "1000066041", Account.Capabilities())
    val destination =
      Account(
        PaymentNetwork.BANK_WIRE,
        "6c87da10-feb8-484f-822c-2083ed762d25",
        "test@mail.com",
        Account.Capabilities()
      )
    var payment: Payment? = null
    assertDoesNotThrow {
      payment =
        service
          .sendPayment(source, destination, CircleAsset.fiatUSD(), BigDecimal.valueOf(0.91))
          .block()
    }

    assertEquals("c58e2613-a808-4075-956c-e576787afb3b", payment?.id)
    assertEquals(merchantAccount, payment?.sourceAccount)
    assertEquals(
      Account(
        PaymentNetwork.BANK_WIRE,
        "6c87da10-feb8-484f-822c-2083ed762d25",
        Account.Capabilities(PaymentNetwork.BANK_WIRE)
      ),
      payment?.destinationAccount
    )
    assertEquals(Balance("0.91", "iso4217:USD"), payment?.balance)
    assertEquals(Payment.Status.PENDING, payment?.status)
    assertNull(payment?.errorCode)

    val wantCreateDate = instantFromString("2021-11-25T15:43:03.477Z")
    assertEquals(wantCreateDate, payment?.createdAt)
    val wantUpdateDate = instantFromString("2021-11-25T16:10:01.618Z")
    assertEquals(wantUpdateDate, payment?.updatedAt)

    val wantMap: Map<String, *> =
      object : HashMap<String, Any?>() {
        init {
          put("id", "c58e2613-a808-4075-956c-e576787afb3b")
          put(
            "amount",
            object : HashMap<String, Any?>() {
              init {
                put("amount", "0.91")
                put("currency", "USD")
              }
            }
          )
          put(
            "fees",
            object : HashMap<String, Any?>() {
              init {
                put("amount", "0.09")
                put("currency", "USD")
              }
            }
          )
          put("status", "pending")
          put("sourceWalletId", "1000066041")
          put(
            "destination",
            object : HashMap<String, Any?>() {
              init {
                put("type", "wire")
                put("id", "6c87da10-feb8-484f-822c-2083ed762d25")
                put("name", "JPMORGAN CHASE BANK, NA ****6789")
              }
            }
          )
          put("createDate", "2021-11-25T15:43:03.477Z")
          put("updateDate", "2021-11-25T16:10:01.618Z")
        }
      }
    assertEquals(wantMap, payment?.originalResponse)

    assertEquals(1, server.requestCount)
    val request = server.takeRequest()
    assertThat(request.path, CoreMatchers.endsWith("/v1/payouts"))
    assertEquals("POST", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", request.headers["Authorization"])
    val gotBody = request.body.readUtf8()
    val wantBody =
      """{
            "source": {
                "type": "wallet",
                "id": "1000066041"
            },
            "destination": {
                "type": "wire",
                "id": "6c87da10-feb8-484f-822c-2083ed762d25"
            },
            "amount": {
                "amount": "0.91",
                "currency": "USD"
            },
            "metadata": {
                "beneficiaryEmail": "test@mail.com"
            }
        }""".trimIndent()
    JSONAssert.assertEquals(wantBody, gotBody, false)
  }

  @Test
  fun test_horizon() {
    val type = (object : TypeToken<Page<OperationResponse>>() {}).type
    val mockStellarPaymentResponsePage: Page<OperationResponse> =
      GsonUtils.getInstance().fromJson(mockStellarPaymentResponsePageBody, type)
    val mockHorizonServer = mockk<Server>()
    every { mockHorizonServer.payments().forTransaction(any()).execute() } returns
      mockStellarPaymentResponsePage

    // when
    val result = mockHorizonServer.payments().forTransaction("foo bar").execute()

    // then
    verify { mockHorizonServer.payments().forTransaction(any()).execute() }
    assertEquals(mockStellarPaymentResponsePage, result)
  }

  @Test
  fun test_getTransfers() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/transfers?pageSize=50&walletId=1000066041" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{    
                    "data": [
                      $mockWalletToWalletTransferJson,
                      $mockStellarToWalletTransferJson,
                      $mockWalletToStellarTransferJson
                    ]
                  }""".trimIndent()
                )
            "/v1/configuration" -> return getDistAccountIdMockResponse()
            "/transactions/fb8947c67856d8eb444211c1927d92bcf14abcfb34cdd27fc9e604b15d208fd1/payments" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(mockStellarPaymentResponsePageBody)
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    var paymentHistory: PaymentHistory? = null
    val service = this.service as CirclePaymentService
    val getTransfersMono = service.getTransfers("1000066041", null, null, null)
    assertDoesNotThrow {
      paymentHistory =
        getTransfersMono.block()!!.toPaymentHistory(50, merchantAccount, "1000066041")
    }

    val wantPaymentHistory = PaymentHistory(merchantAccount)
    wantPaymentHistory.beforeCursor = "c58e2613-a808-4075-956c-e576787afb3b"

    val gson = Gson()
    val type = object : TypeToken<Map<String?, *>?>() {}.type

    val p1 = Payment()
    p1.id = "c58e2613-a808-4075-956c-e576787afb3b"
    p1.sourceAccount = merchantAccount
    p1.destinationAccount =
      Account(PaymentNetwork.CIRCLE, "1000067536", CircleWallet.defaultCapabilities())
    p1.balance = Balance("0.91", "circle:USD")
    p1.status = Payment.Status.PENDING
    p1.createdAt = instantFromString("2022-02-07T19:50:23.408Z")
    p1.updatedAt = instantFromString("2022-02-07T19:50:23.408Z")
    p1.originalResponse = gson.fromJson(mockWalletToWalletTransferJson, type)
    wantPaymentHistory.payments.add(p1)

    val p2 = Payment()
    p2.id = "7f131f58-a8a0-3dc2-be05-6a015c69de35"
    p2.sourceAccount =
      Account(
        PaymentNetwork.STELLAR,
        "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
        Account.Capabilities(PaymentNetwork.STELLAR)
      )
    p2.destinationAccount = merchantAccount
    p2.balance = Balance("1.50", "circle:USD")
    p2.idTag = "fb8947c67856d8eb444211c1927d92bcf14abcfb34cdd27fc9e604b15d208fd1"
    p2.status = Payment.Status.SUCCESSFUL
    p2.createdAt = instantFromString("2022-02-07T18:02:17.999Z")
    p2.updatedAt = instantFromString("2022-02-07T18:02:17.999Z")
    p2.originalResponse = gson.fromJson(mockStellarToWalletTransferJson, type)
    wantPaymentHistory.payments.add(p2)

    val p3 = Payment()
    p3.id = "a8997020-3da7-4543-bc4a-5ae8c7ce346d"
    p3.sourceAccount = merchantAccount
    p3.destinationAccount =
      Account(
        PaymentNetwork.STELLAR,
        "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
        Account.Capabilities(PaymentNetwork.STELLAR)
      )
    p3.balance =
      Balance("1.00", "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
    p3.idTag = "5239ee055b1083231c6bdaaa921d3e4b3bc090577fbd909815bd5d7fe68091ef"
    p3.status = Payment.Status.SUCCESSFUL
    p3.createdAt = instantFromString("2022-01-01T01:01:01.544Z")
    p3.updatedAt = instantFromString("2022-01-01T01:01:01.544Z")
    p3.originalResponse = gson.fromJson(mockWalletToStellarTransferJson, type)
    wantPaymentHistory.payments.add(p3)

    assertEquals(wantPaymentHistory, paymentHistory)
  }

  @Test
  fun test_getTransfers_paginationResponse() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/transfers?pageSize=2&walletId=1000066041" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{    
                    "data": [
                      $mockWalletToWalletTransferJson,
                      $mockWalletToStellarTransferJson
                    ]
                  }""".trimIndent()
                )
            "/v1/configuration" -> return getDistAccountIdMockResponse()
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    // Let's use reflection to access the private method
    val getTransfersMono =
      (service as CirclePaymentService).getTransfers("1000066041", null, null, 2)
    var paymentHistory: PaymentHistory? = null
    assertDoesNotThrow {
      paymentHistory = getTransfersMono.block()!!.toPaymentHistory(2, merchantAccount, "1000066041")
    }

    val wantPaymentHistory = PaymentHistory(merchantAccount)
    wantPaymentHistory.beforeCursor = "c58e2613-a808-4075-956c-e576787afb3b"
    wantPaymentHistory.afterCursor = "a8997020-3da7-4543-bc4a-5ae8c7ce346d"

    val gson = Gson()
    val type = object : TypeToken<Map<String?, *>?>() {}.type

    val p1 = Payment()
    p1.id = "c58e2613-a808-4075-956c-e576787afb3b"
    p1.sourceAccount = merchantAccount
    p1.destinationAccount =
      Account(PaymentNetwork.CIRCLE, "1000067536", CircleWallet.defaultCapabilities())
    p1.balance = Balance("0.91", "circle:USD")
    p1.status = Payment.Status.PENDING
    p1.createdAt = instantFromString("2022-02-07T19:50:23.408Z")
    p1.updatedAt = instantFromString("2022-02-07T19:50:23.408Z")
    p1.originalResponse = gson.fromJson(mockWalletToWalletTransferJson, type)
    wantPaymentHistory.payments.add(p1)

    val p2 = Payment()
    p2.id = "a8997020-3da7-4543-bc4a-5ae8c7ce346d"
    p2.sourceAccount = merchantAccount
    p2.destinationAccount =
      Account(
        PaymentNetwork.STELLAR,
        "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
        Account.Capabilities(PaymentNetwork.STELLAR)
      )
    p2.balance =
      Balance("1.00", "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
    p2.idTag = "5239ee055b1083231c6bdaaa921d3e4b3bc090577fbd909815bd5d7fe68091ef"
    p2.status = Payment.Status.SUCCESSFUL
    p2.createdAt = instantFromString("2022-01-01T01:01:01.544Z")
    p2.updatedAt = instantFromString("2022-01-01T01:01:01.544Z")
    p2.originalResponse = gson.fromJson(mockWalletToStellarTransferJson, type)
    wantPaymentHistory.payments.add(p2)

    assertEquals(wantPaymentHistory, paymentHistory)
  }

  @Test
  fun test_getPayouts() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/payouts?pageSize=50&source=1000066041" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{    
                    "data": [
                      $mockWalletToWirePayoutJson
                    ]
                  }""".trimIndent()
                )
            "/v1/configuration" -> return getDistAccountIdMockResponse()
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    val getPayoutsMono = (service as CirclePaymentService).getPayouts("1000066041", null, null, 50)
    var paymentHistory: PaymentHistory? = null
    assertDoesNotThrow {
      paymentHistory = getPayoutsMono.block()!!.toPaymentHistory(50, merchantAccount)
    }

    val wantPaymentHistory = PaymentHistory(merchantAccount)
    wantPaymentHistory.beforeCursor = "6588a352-5131-4711-a264-e405f38d752d"

    val p = Payment()
    p.id = "6588a352-5131-4711-a264-e405f38d752d"
    p.sourceAccount = merchantAccount
    p.destinationAccount =
      Account(
        PaymentNetwork.BANK_WIRE,
        "6c87da10-feb8-484f-822c-2083ed762d25",
        Account.Capabilities(PaymentNetwork.BANK_WIRE)
      )
    p.balance = Balance("3.00", "iso4217:USD")
    p.status = Payment.Status.SUCCESSFUL
    p.createdAt = instantFromString("2022-02-03T15:41:25.286Z")
    p.updatedAt = instantFromString("2022-02-03T16:00:31.697Z")
    val gson = Gson()
    val type = object : TypeToken<Map<String?, *>?>() {}.type
    p.originalResponse = gson.fromJson(mockWalletToWirePayoutJson, type)
    wantPaymentHistory.payments.add(p)

    assertEquals(wantPaymentHistory, paymentHistory)
  }

  @Test
  fun test_getPayouts_paginationResponse() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/payouts?pageSize=1&source=1000066041" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{    
                    "data": [
                      $mockWalletToWirePayoutJson
                    ]
                  }""".trimIndent()
                )
            "/v1/configuration" -> return getDistAccountIdMockResponse()
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    val getPayoutsMono = (service as CirclePaymentService).getPayouts("1000066041", null, null, 1)
    var paymentHistory: PaymentHistory? = null
    assertDoesNotThrow {
      paymentHistory = getPayoutsMono.block()!!.toPaymentHistory(1, merchantAccount)
    }

    val wantPaymentHistory = PaymentHistory(merchantAccount)
    wantPaymentHistory.beforeCursor = "6588a352-5131-4711-a264-e405f38d752d"
    wantPaymentHistory.afterCursor = "6588a352-5131-4711-a264-e405f38d752d"

    val p = Payment()
    p.id = "6588a352-5131-4711-a264-e405f38d752d"
    p.sourceAccount = merchantAccount
    p.destinationAccount =
      Account(
        PaymentNetwork.BANK_WIRE,
        "6c87da10-feb8-484f-822c-2083ed762d25",
        Account.Capabilities(PaymentNetwork.BANK_WIRE)
      )
    p.balance = Balance("3.00", "iso4217:USD")
    p.status = Payment.Status.SUCCESSFUL
    p.createdAt = instantFromString("2022-02-03T15:41:25.286Z")
    p.updatedAt = instantFromString("2022-02-03T16:00:31.697Z")
    val gson = Gson()
    val type = object : TypeToken<Map<String?, *>?>() {}.type
    p.originalResponse = gson.fromJson(mockWalletToWirePayoutJson, type)
    wantPaymentHistory.payments.add(p)

    assertEquals(wantPaymentHistory, paymentHistory)
  }

  @Test
  fun test_getIncomingPayments() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/payments?pageSize=50" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{    
                    "data": [
                      $mockWireToWalletPaymentJson
                    ]
                  }""".trimIndent()
                )
            "/v1/configuration" -> return getDistAccountIdMockResponse()
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    val getPaymentsMono =
      (service as CirclePaymentService).getIncomingPayments("1000066041", null, null, 50)
    var paymentHistory: PaymentHistory? = null
    assertDoesNotThrow {
      paymentHistory = getPaymentsMono.block()!!.toPaymentHistory(50, merchantAccount)
    }

    val wantPaymentHistory = PaymentHistory(merchantAccount)
    wantPaymentHistory.beforeCursor = "acc622bf-89e1-447c-8588-1bdead8e41a3"

    val p = Payment()
    p.id = "acc622bf-89e1-447c-8588-1bdead8e41a3"
    p.sourceAccount =
      Account(
        PaymentNetwork.BANK_WIRE,
        "a4e76642-81c5-47ca-9229-ebd64efd74a7",
        Account.Capabilities(PaymentNetwork.BANK_WIRE)
      )
    p.destinationAccount = merchantAccount
    p.balance = Balance("1000.00", "circle:USD")
    p.status = Payment.Status.SUCCESSFUL
    p.createdAt = instantFromString("2022-02-21T19:20:01.438Z")
    p.updatedAt = instantFromString("2022-02-21T19:28:01.901Z")
    val gson = Gson()
    val type = object : TypeToken<Map<String?, *>?>() {}.type
    p.originalResponse = gson.fromJson(mockWireToWalletPaymentJson, type)
    wantPaymentHistory.payments.add(p)

    assertEquals(wantPaymentHistory, paymentHistory)
  }

  @Test
  fun test_getIncomingPayments_paginationResponse() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/payments?pageSize=1" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{    
                    "data": [
                      $mockWireToWalletPaymentJson
                    ]
                  }""".trimIndent()
                )
            "/v1/configuration" -> return getDistAccountIdMockResponse()
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    val getPaymentsMono =
      (service as CirclePaymentService).getIncomingPayments("1000066041", null, null, 1)
    var paymentHistory: PaymentHistory? = null
    assertDoesNotThrow {
      paymentHistory = getPaymentsMono.block()!!.toPaymentHistory(1, merchantAccount)
    }

    val wantPaymentHistory = PaymentHistory(merchantAccount)
    wantPaymentHistory.beforeCursor = "acc622bf-89e1-447c-8588-1bdead8e41a3"
    wantPaymentHistory.afterCursor = "acc622bf-89e1-447c-8588-1bdead8e41a3"

    val p = Payment()
    p.id = "acc622bf-89e1-447c-8588-1bdead8e41a3"
    p.sourceAccount =
      Account(
        PaymentNetwork.BANK_WIRE,
        "a4e76642-81c5-47ca-9229-ebd64efd74a7",
        Account.Capabilities(PaymentNetwork.BANK_WIRE)
      )
    p.destinationAccount = merchantAccount
    p.balance = Balance("1000.00", "circle:USD")
    p.status = Payment.Status.SUCCESSFUL
    p.createdAt = instantFromString("2022-02-21T19:20:01.438Z")
    p.updatedAt = instantFromString("2022-02-21T19:28:01.901Z")
    val gson = Gson()
    val type = object : TypeToken<Map<String?, *>?>() {}.type
    p.originalResponse = gson.fromJson(mockWireToWalletPaymentJson, type)
    wantPaymentHistory.payments.add(p)

    assertEquals(wantPaymentHistory, paymentHistory)
  }

  @ParameterizedTest
  @CsvSource(
    value =
      [
        "transfers,,,/v1/transfers?pageSize=1&walletId=1000066041",
        "transfers,before,,/v1/transfers?pageSize=1&walletId=1000066041&pageBefore=before",
        "transfers,,after,/v1/transfers?pageSize=1&walletId=1000066041&pageAfter=after",
        "transfers,before,after,/v1/transfers?pageSize=1&walletId=1000066041&pageAfter=after",
        "payouts,,,/v1/payouts?pageSize=1&source=1000066041",
        "payouts,before,,/v1/payouts?pageSize=1&source=1000066041&pageBefore=before",
        "payouts,,after,/v1/payouts?pageSize=1&source=1000066041&pageAfter=after",
        "payouts,before,after,/v1/payouts?pageSize=1&source=1000066041&pageAfter=after",
        "payments,,,/v1/payments?pageSize=1&source=1000066041",
        "payments,before,,/v1/payments?pageSize=1&source=1000066041&pageBefore=before",
        "payments,,after,/v1/payments?pageSize=1&source=1000066041&pageAfter=after",
        "payments,before,after,/v1/payments?pageSize=1&source=1000066041&pageAfter=after",
      ]
  )
  fun test_getTransfersOrPayoutsOrPayments_paginationRequestUri(
    uri: String,
    beforeCursor: String?,
    afterCursor: String?
  ) {
    val dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          if (request.path!!.startsWith("/v1/$uri")) {
            return MockResponse()
              .addHeader("Content-Type", "application/json")
              .setBody("""{
                "data": []
              }""".trimIndent())
          }

          if (request.path.equals("/v1/configuration")) {
            return getDistAccountIdMockResponse()
          }

          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    var paymentHistory: PaymentHistory? = null
    when (uri) {
      "transfers" -> {
        assertDoesNotThrow {
          paymentHistory =
            (service as CirclePaymentService)
                .getTransfers("1000066041", beforeCursor, afterCursor, 1)
                .block()!!
              .toPaymentHistory(1, merchantAccount, "1000066041")
        }
      }
      "payouts" -> {
        assertDoesNotThrow {
          paymentHistory =
            (service as CirclePaymentService)
                .getPayouts("1000066041", beforeCursor, afterCursor, 1)
                .block()!!
              .toPaymentHistory(1, merchantAccount)
        }
      }
      "payments" -> {
        assertDoesNotThrow {
          paymentHistory =
            (service as CirclePaymentService)
                .getIncomingPayments("1000066041", beforeCursor, afterCursor, 1)
                .block()!!
              .toPaymentHistory(1, merchantAccount)
        }
      }
      else -> {
        throw RuntimeException("INVALID URI FOR TEST")
      }
    }

    val wantPaymentHistory = PaymentHistory(merchantAccount)
    assertEquals(wantPaymentHistory, paymentHistory)

    val request = server.takeRequest()
    assertEquals("GET", request.method)
    assertEquals("Bearer <secret-key>", request.headers["Authorization"])
  }

  @Test
  fun test_getAccountHistory() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/configuration" -> return getDistAccountIdMockResponse()
            "/v1/transfers?pageSize=50&walletId=1000066041" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{
                      "data": [
                        $mockWalletToWalletTransferJson,
                        $mockStellarToWalletTransferJson,
                        $mockWalletToStellarTransferJson
                      ]
                    }""".trimIndent()
                )
            "/v1/payouts?pageSize=50&source=1000066041" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{
                      "data": [
                        $mockWalletToWirePayoutJson
                      ]
                    }""".trimIndent()
                )
            "/v1/payments?pageSize=50" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(
                  """{
                      "data": [
                        $mockWireToWalletPaymentJson
                      ]
                    }""".trimIndent()
                )
            "/transactions/fb8947c67856d8eb444211c1927d92bcf14abcfb34cdd27fc9e604b15d208fd1/payments" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(mockStellarPathPaymentResponsePageBody)
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    var paymentHistory: PaymentHistory? = null
    val getAccountHistoryMono = service.getAccountPaymentHistory("1000066041", null, null)
    assertDoesNotThrow { paymentHistory = getAccountHistoryMono.block() }

    val wantPaymentHistory = PaymentHistory(merchantAccount)
    wantPaymentHistory.beforeCursor =
      "c58e2613-a808-4075-956c-e576787afb3b:6588a352-5131-4711-a264-e405f38d752d:acc622bf-89e1-447c-8588-1bdead8e41a3"

    val gson = Gson()
    val type = object : TypeToken<Map<String?, *>?>() {}.type

    val p0 = Payment()
    p0.id = "acc622bf-89e1-447c-8588-1bdead8e41a3"
    p0.sourceAccount =
      Account(
        PaymentNetwork.BANK_WIRE,
        "a4e76642-81c5-47ca-9229-ebd64efd74a7",
        Account.Capabilities(PaymentNetwork.BANK_WIRE)
      )
    p0.destinationAccount = merchantAccount
    p0.balance = Balance("1000.00", "circle:USD")
    p0.status = Payment.Status.SUCCESSFUL
    p0.createdAt = instantFromString("2022-02-21T19:20:01.438Z")
    p0.updatedAt = instantFromString("2022-02-21T19:28:01.901Z")
    p0.originalResponse = gson.fromJson(mockWireToWalletPaymentJson, type)
    wantPaymentHistory.payments.add(p0)

    val p1 = Payment()
    p1.id = "c58e2613-a808-4075-956c-e576787afb3b"
    p1.sourceAccount = merchantAccount
    p1.destinationAccount =
      Account(PaymentNetwork.CIRCLE, "1000067536", CircleWallet.defaultCapabilities())
    p1.balance = Balance("0.91", "circle:USD")
    p1.status = Payment.Status.PENDING
    p1.createdAt = instantFromString("2022-02-07T19:50:23.408Z")
    p1.updatedAt = instantFromString("2022-02-07T19:50:23.408Z")
    p1.originalResponse = gson.fromJson(mockWalletToWalletTransferJson, type)
    wantPaymentHistory.payments.add(p1)

    val p2 = Payment()
    p2.id = "7f131f58-a8a0-3dc2-be05-6a015c69de35"
    p2.sourceAccount =
      Account(
        PaymentNetwork.STELLAR,
        "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
        Account.Capabilities(PaymentNetwork.STELLAR)
      )
    p2.destinationAccount = merchantAccount
    p2.balance = Balance("1.50", "circle:USD")
    p2.idTag = "fb8947c67856d8eb444211c1927d92bcf14abcfb34cdd27fc9e604b15d208fd1"
    p2.status = Payment.Status.SUCCESSFUL
    p2.createdAt = instantFromString("2022-02-07T18:02:17.999Z")
    p2.updatedAt = instantFromString("2022-02-07T18:02:17.999Z")
    p2.originalResponse = gson.fromJson(mockStellarToWalletTransferJson, type)
    wantPaymentHistory.payments.add(p2)

    val p3 = Payment()
    p3.id = "6588a352-5131-4711-a264-e405f38d752d"
    p3.sourceAccount = merchantAccount
    p3.destinationAccount =
      Account(
        PaymentNetwork.BANK_WIRE,
        "6c87da10-feb8-484f-822c-2083ed762d25",
        Account.Capabilities(PaymentNetwork.BANK_WIRE)
      )
    p3.balance = Balance("3.00", "iso4217:USD")
    p3.status = Payment.Status.SUCCESSFUL
    p3.createdAt = instantFromString("2022-02-03T15:41:25.286Z")
    p3.updatedAt = instantFromString("2022-02-03T16:00:31.697Z")
    p3.originalResponse = gson.fromJson(mockWalletToWirePayoutJson, type)
    wantPaymentHistory.payments.add(p3)

    val p4 = Payment()
    p4.id = "a8997020-3da7-4543-bc4a-5ae8c7ce346d"
    p4.sourceAccount = merchantAccount
    p4.destinationAccount =
      Account(
        PaymentNetwork.STELLAR,
        "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
        Account.Capabilities(PaymentNetwork.STELLAR)
      )
    p4.balance =
      Balance("1.00", "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
    p4.idTag = "5239ee055b1083231c6bdaaa921d3e4b3bc090577fbd909815bd5d7fe68091ef"
    p4.status = Payment.Status.SUCCESSFUL
    p4.createdAt = instantFromString("2022-01-01T01:01:01.544Z")
    p4.updatedAt = instantFromString("2022-01-01T01:01:01.544Z")
    p4.originalResponse = gson.fromJson(mockWalletToStellarTransferJson, type)
    wantPaymentHistory.payments.add(p4)

    assertEquals(wantPaymentHistory, paymentHistory)
  }

  @Test
  fun test_getListOfAddresses() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/wallets/1000066041/addresses" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(mockGetListOfAddressesBody)
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    val service = this.service as CirclePaymentService
    var response: CircleListResponse<CircleBlockchainAddress>? = null
    assertDoesNotThrow { response = service.getListOfAddresses("1000066041").block() }

    val wantAddresses = ArrayList<CircleBlockchainAddress>()
    wantAddresses.add(
      CircleBlockchainAddress(
        "GAYF33NNNMI2Z6VNRFXQ64D4E4SF77PM46NW3ZUZEEU5X7FCHAZCMHKU",
        "2454278437550473431",
        "USD",
        "XLM"
      )
    )
    wantAddresses.add(
      CircleBlockchainAddress(
        "GAYF33NNNMI2Z6VNRFXQ64D4E4SF77PM46NW3ZUZEEU5X7FCHAZCMHKU",
        "4560730744420812493",
        "USD",
        "XLM"
      )
    )
    wantAddresses.add(
      CircleBlockchainAddress("TLiSMwSrVp8YZaqt7RRcAZoptT1kmpA9sC", null, "USD", "TRX")
    )
    val wantResponse = CircleListResponse<CircleBlockchainAddress>()
    wantResponse.data = wantAddresses

    assertEquals(wantResponse, response)

    val getRequest = server.takeRequest()
    assertThat(getRequest.path, CoreMatchers.endsWith("/v1/wallets/1000066041/addresses"))
    assertEquals("GET", getRequest.method)
    assertEquals("application/json", getRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", getRequest.headers["Authorization"])
  }

  @Test
  fun test_createNewStellarAddress() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/wallets/1000066041/addresses" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("""{"data":$mockAddressJson}""")
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    val service = this.service as CirclePaymentService
    var response: CircleDetailResponse<CircleBlockchainAddress>? = null
    assertDoesNotThrow { response = service.createNewStellarAddress("1000066041").block() }

    val wantResponse = CircleDetailResponse<CircleBlockchainAddress>()
    wantResponse.data =
      CircleBlockchainAddress(
        "GAYF33NNNMI2Z6VNRFXQ64D4E4SF77PM46NW3ZUZEEU5X7FCHAZCMHKU",
        "2454278437550473431",
        "USD",
        "XLM"
      )

    assertEquals(wantResponse, response)

    val request = server.takeRequest()
    assertThat(request.path, CoreMatchers.endsWith("/v1/wallets/1000066041/addresses"))
    assertEquals("POST", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", request.headers["Authorization"])
    val gotBody = request.body.readUtf8()
    val wantBody = """{"currency": "USD", "chain": "XLM"}"""
    JSONAssert.assertEquals(wantBody, gotBody, false)
  }

  @Test
  fun test_getOrCreateStellarAddress() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {

          if (request.path == "/v1/wallets/1000066041/addresses" && request.method == "GET") {
            return MockResponse()
              .addHeader("Content-Type", "application/json")
              .setBody("""{"data": []}""")
          }

          if (request.path == "/v1/wallets/1000066041/addresses" && request.method == "POST") {
            return MockResponse()
              .addHeader("Content-Type", "application/json")
              .setBody("""{"data":$mockAddressJson}""")
          }

          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    val service = this.service as CirclePaymentService
    var address: CircleBlockchainAddress? = null
    assertDoesNotThrow { address = service.getOrCreateStellarAddress("1000066041").block() }

    val wantAddress =
      CircleBlockchainAddress(
        "GAYF33NNNMI2Z6VNRFXQ64D4E4SF77PM46NW3ZUZEEU5X7FCHAZCMHKU",
        "2454278437550473431",
        "USD",
        "XLM"
      )
    assertEquals(wantAddress, address)

    assertEquals(2, server.requestCount)

    val getRequest = server.takeRequest()
    assertThat(getRequest.path, CoreMatchers.endsWith("/v1/wallets/1000066041/addresses"))
    assertEquals("GET", getRequest.method)
    assertEquals("application/json", getRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", getRequest.headers["Authorization"])

    val request = server.takeRequest()
    assertThat(request.path, CoreMatchers.endsWith("/v1/wallets/1000066041/addresses"))
    assertEquals("POST", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", request.headers["Authorization"])
    val gotBody = request.body.readUtf8()
    val wantBody = """{"currency": "USD", "chain": "XLM"}"""
    JSONAssert.assertEquals(wantBody, gotBody, false)
  }

  @Test
  fun test_getWireDepositInstructions() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/configuration" -> return getDistAccountIdMockResponse()
            "/v1/banks/wires/bank-id-here/instructions" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(mockGetWireDepositInstructionsBody)
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    val service = this.service as CirclePaymentService
    var response: CircleDetailResponse<CircleWireDepositInstructions>? = null
    assertDoesNotThrow {
      response = service.getWireDepositInstructions("1000066041", "bank-id-here").block()
    }

    val wantWireInstructions = CircleWireDepositInstructions()
    wantWireInstructions.trackingRef = "CIR2KMMZEJ"
    val wantBeneficiary = CircleWireDepositInstructions.Beneficiary()
    wantBeneficiary.name = "CIRCLE INTERNET FINANCIAL INC"
    wantBeneficiary.address1 = "1 MAIN STREET"
    wantBeneficiary.address2 = "SUITE 1"
    wantWireInstructions.beneficiary = wantBeneficiary
    val wantBeneficiaryBank = CircleWireDepositInstructions.BeneficiaryBank()
    wantBeneficiaryBank.name = "CRYPTO BANK"
    wantBeneficiaryBank.address = "1 MONEY STREET"
    wantBeneficiaryBank.city = "NEW YORK"
    wantBeneficiaryBank.postalCode = "1001"
    wantBeneficiaryBank.country = "US"
    wantBeneficiaryBank.swiftCode = "CRYPTO99"
    wantBeneficiaryBank.routingNumber = "999999999"
    wantBeneficiaryBank.accountNumber = "1000000001"
    wantWireInstructions.beneficiaryBank = wantBeneficiaryBank

    val wantResponse = CircleDetailResponse<CircleWireDepositInstructions>()
    wantResponse.data = wantWireInstructions
    assertEquals(wantResponse, response)

    assertEquals(2, server.requestCount)

    val validateSecretKeyRequest = server.takeRequest()
    assertEquals("GET", validateSecretKeyRequest.method)
    assertEquals("application/json", validateSecretKeyRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", validateSecretKeyRequest.headers["Authorization"])
    assertTrue(validateSecretKeyRequest.path!!.endsWith("/v1/configuration"))

    val wireInstructionsRequest = server.takeRequest()
    assertThat(
      wireInstructionsRequest.path,
      CoreMatchers.endsWith("/v1/banks/wires/bank-id-here/instructions")
    )
    assertEquals("GET", wireInstructionsRequest.method)
    assertEquals("application/json", wireInstructionsRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", wireInstructionsRequest.headers["Authorization"])
  }

  @Test
  fun test_getWireDepositInstructions_notTheDistributionAccount() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/configuration" -> return getDistAccountIdMockResponse()
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    val service = this.service as CirclePaymentService
    val ex: HttpException = assertThrows {
      service.getWireDepositInstructions("1000646072", "bank-id-here").block()
    }
    val wantException =
      HttpException(400, "in circle, only the distribution account id can receive wire payments")
    assertEquals(wantException, ex)
  }

  @Test
  fun test_getDepositInstructions_parameterValidation() {
    // empty beneficiary account id
    var config = DepositRequirements(null, null, null, null)
    var ex: HttpException = assertThrows { service.getDepositInstructions(config).block() }
    assertEquals(HttpException(400, "beneficiary account id cannot be empty"), ex)

    // invalid currency name
    config = DepositRequirements("1000066041", null, null, null)
    ex = assertThrows { service.getDepositInstructions(config).block() }
    assertEquals(
      HttpException(400, "the only receiving currency in a circle account is \"circle:USD\""),
      ex
    )

    // missing bank id
    config =
      DepositRequirements("1000066041", null, PaymentNetwork.BANK_WIRE, CircleAsset.circleUSD())
    ex = assertThrows { service.getDepositInstructions(config).block() }
    assertEquals(
      HttpException(
        400,
        "please provide a valid Circle bank id for the intermediaryAccountId field when requesting instructions for bank wire deposits"
      ),
      ex
    )
  }

  @ParameterizedTest
  @NullSource
  @EnumSource(
    value = PaymentNetwork::class,
    mode = EnumSource.Mode.EXCLUDE,
    names = ["STELLAR", "CIRCLE", "BANK_WIRE"]
  )
  fun test_getDepositInstructions_parameterValidation_supportedNetworks(
    paymentNetwork: PaymentNetwork?
  ) {
    // unsupported intermediary payment network
    val config = DepositRequirements("1000066041", null, paymentNetwork, CircleAsset.circleUSD())
    val ex: HttpException = assertThrows { service.getDepositInstructions(config).block() }
    assertEquals(
      HttpException(
        400,
        """the only supported intermediary payment networks are "stellar", "circle" and "bank_wire""""
      ),
      ex
    )
  }

  @Test
  fun test_getDepositInstructions_circle() {
    var instructions: DepositInstructions? = null
    val config =
      DepositRequirements("1000066041", null, PaymentNetwork.CIRCLE, CircleAsset.circleUSD())
    assertDoesNotThrow { instructions = service.getDepositInstructions(config).block() }

    val wantInstructions =
      DepositInstructions(
        "1000066041",
        null,
        PaymentNetwork.CIRCLE,
        "1000066041",
        null,
        PaymentNetwork.CIRCLE,
        "circle:USD",
        null
      )
    assertEquals(wantInstructions, instructions)
    assertEquals(0, server.requestCount)
  }

  @Test
  fun test_getDepositInstructions_stellar() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {

          if (request.path == "/v1/wallets/1000066041/addresses" && request.method == "GET") {
            return MockResponse()
              .addHeader("Content-Type", "application/json")
              .setBody("""{"data": []}""")
          }

          if (request.path == "/v1/wallets/1000066041/addresses" && request.method == "POST") {
            return MockResponse()
              .addHeader("Content-Type", "application/json")
              .setBody("""{"data":$mockAddressJson}""")
          }

          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    var instructions: DepositInstructions? = null
    val config =
      DepositRequirements("1000066041", null, PaymentNetwork.STELLAR, CircleAsset.circleUSD())
    assertDoesNotThrow { instructions = service.getDepositInstructions(config).block() }

    val wantInstructions =
      DepositInstructions(
        "1000066041",
        null,
        PaymentNetwork.CIRCLE,
        "GAYF33NNNMI2Z6VNRFXQ64D4E4SF77PM46NW3ZUZEEU5X7FCHAZCMHKU",
        "2454278437550473431",
        PaymentNetwork.STELLAR,
        CircleAsset.stellarUSDC(Network.TESTNET),
        null
      )
    assertEquals(wantInstructions, instructions)

    assertEquals(2, server.requestCount)

    val getRequest = server.takeRequest()
    assertThat(getRequest.path, CoreMatchers.endsWith("/v1/wallets/1000066041/addresses"))
    assertEquals("GET", getRequest.method)
    assertEquals("application/json", getRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", getRequest.headers["Authorization"])

    val request = server.takeRequest()
    assertThat(request.path, CoreMatchers.endsWith("/v1/wallets/1000066041/addresses"))
    assertEquals("POST", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", request.headers["Authorization"])
    val gotBody = request.body.readUtf8()
    val wantBody = """{"currency": "USD", "chain": "XLM"}"""
    JSONAssert.assertEquals(wantBody, gotBody, false)
  }

  @Test
  fun test_getDepositInstructions_wire() {
    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
          when (request.path) {
            "/v1/configuration" -> return getDistAccountIdMockResponse()
            "/v1/banks/wires/bank-id-here/instructions" ->
              return MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(mockGetWireDepositInstructionsBody)
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    var instructions: DepositInstructions? = null
    val config =
      DepositRequirements(
        "1000066041",
        null,
        PaymentNetwork.BANK_WIRE,
        "bank-id-here",
        CircleAsset.circleUSD()
      )
    assertDoesNotThrow { instructions = service.getDepositInstructions(config).block() }

    val wantInstructions =
      DepositInstructions(
        "1000066041",
        null,
        PaymentNetwork.CIRCLE,
        "CIR2KMMZEJ",
        null,
        PaymentNetwork.BANK_WIRE,
        "iso4217:USD",
        mapOf(
          "trackingRef" to "CIR2KMMZEJ",
          "beneficiary" to
            mapOf(
              "name" to "CIRCLE INTERNET FINANCIAL INC",
              "address1" to "1 MAIN STREET",
              "address2" to "SUITE 1"
            ),
          "beneficiaryBank" to
            mapOf(
              "name" to "CRYPTO BANK",
              "address" to "1 MONEY STREET",
              "city" to "NEW YORK",
              "postalCode" to "1001",
              "country" to "US",
              "swiftCode" to "CRYPTO99",
              "routingNumber" to "999999999",
              "accountNumber" to "1000000001"
            )
        )
      )
    assertEquals(wantInstructions, instructions)

    assertEquals(2, server.requestCount)

    val validateSecretKeyRequest = server.takeRequest()
    assertEquals("GET", validateSecretKeyRequest.method)
    assertEquals("application/json", validateSecretKeyRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", validateSecretKeyRequest.headers["Authorization"])
    assertTrue(validateSecretKeyRequest.path!!.endsWith("/v1/configuration"))

    val wireInstructionsRequest = server.takeRequest()
    assertThat(
      wireInstructionsRequest.path,
      CoreMatchers.endsWith("/v1/banks/wires/bank-id-here/instructions")
    )
    assertEquals("GET", wireInstructionsRequest.method)
    assertEquals("application/json", wireInstructionsRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", wireInstructionsRequest.headers["Authorization"])
  }

  @Test
  fun test_errorHandling() {
    val badRequestResponse =
      MockResponse()
        .setResponseCode(400)
        .addHeader("Content-Type", "application/json")
        .setBody("{\"code\":2,\"message\":\"Request body contains unprocessable entity.\"}")
    val validateSecretKeyResponse = getDistAccountIdMockResponse()
    val mainAccountResponse =
      MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody(
          """{
                        "data":{
                            "available":[],
                            "unsettled": [
                                {
                                    "amount":"100.00",
                                    "currency":"USD"
                                }
                            ]
                        }
                    }"""
        )
    val emptyListResponse =
      MockResponse().addHeader("Content-Type", "application/json").setBody("""{"data":[]}""")

    // Access private method getMainAccountBalances
    val getMerchantAccountUnsettledBalancesMethod: Method =
      CirclePaymentService::class.java.getDeclaredMethod("getMerchantAccountUnsettledBalances")
    assert(getMerchantAccountUnsettledBalancesMethod.trySetAccessible())

    // Access private method getCircleWallet
    val getCircleWalletMethod: Method =
      CirclePaymentService::class.java.getDeclaredMethod("getCircleWallet", String::class.java)
    assert(getCircleWalletMethod.trySetAccessible())

    // declare aux method
    val validateErrHandling = { testCase: ErrorHandlingTestCase ->
      // run project reactor synchronously
      testCase.prepareMockWebServer(server)
      var request = testCase.requestMono
      val thrown = assertThrows<HttpException> { request.block() }
      assertEquals(HttpException(400, "Request body contains unprocessable entity.", "2"), thrown)

      // run project reactor asynchronously
      var didRunAsyncTask = false
      testCase.prepareMockWebServer(server)
      request =
        testCase.requestMono.onErrorResume { ex ->
          assertInstanceOf(HttpException::class.java, ex)
          assertEquals(HttpException(400, "Request body contains unprocessable entity.", "2"), ex)
          didRunAsyncTask = true
          Mono.empty()
        }
      assertDoesNotThrow { request.block() }
      assertTrue(didRunAsyncTask)
    }

    // --- tests with sync/serial requests ---
    validateErrHandling(ErrorHandlingTestCase(service.ping(), listOf(badRequestResponse)))
    validateErrHandling(
      ErrorHandlingTestCase(service.distributionAccountAddress, listOf(badRequestResponse))
    )
    validateErrHandling(
      ErrorHandlingTestCase(service.getAccount("random_id"), listOf(badRequestResponse))
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        service.getAccount("random_id"),
        listOf(validateSecretKeyResponse, badRequestResponse)
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        getMerchantAccountUnsettledBalancesMethod.invoke(service) as Mono<*>,
        listOf(badRequestResponse)
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(service.createAccount(null), listOf(badRequestResponse))
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        getCircleWalletMethod.invoke(service, "random_id") as Mono<*>,
        listOf(badRequestResponse)
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(service.createAccount(null), listOf(badRequestResponse))
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        (service as CirclePaymentService).getListOfAddresses("any_id"),
        listOf(badRequestResponse)
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        (service as CirclePaymentService).createNewStellarAddress("any_id"),
        listOf(badRequestResponse)
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        (service as CirclePaymentService).getOrCreateStellarAddress("any_id"),
        listOf(emptyListResponse, badRequestResponse)
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        (service as CirclePaymentService).getWireDepositInstructions("1000066041", "any_bank_id"),
        listOf(badRequestResponse)
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        service.getDepositInstructions(
          DepositRequirements("1000066041", PaymentNetwork.STELLAR, CircleAsset.circleUSD())
        ),
        listOf(badRequestResponse)
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        service.getDepositInstructions(
          DepositRequirements(
            "1000066041",
            null,
            PaymentNetwork.BANK_WIRE,
            "bank-id-here",
            CircleAsset.circleUSD()
          )
        ),
        listOf(badRequestResponse)
      )
    )
    // --- tests with async/parallel requests ---
    // ATTENTION, make sure to run parallel tests at the end, if you try to run a serial
    // test
    // after a parallel
    // test, the server dispatcher will throw an exception.
    validateErrHandling(
      ErrorHandlingTestCase(
        service.getAccount("1000066041"),
        hashMapOf(
          "/v1/configuration" to validateSecretKeyResponse,
          "/v1/businessAccount/balances" to mainAccountResponse,
          "/v1/wallets/1000066041" to badRequestResponse
        )
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        service.sendPayment(
          Account(PaymentNetwork.CIRCLE, "1000066041", Account.Capabilities()),
          Account(PaymentNetwork.CIRCLE, "1000067536", Account.Capabilities()),
          CircleAsset.circleUSD(),
          BigDecimal.valueOf(1)
        ),
        hashMapOf(
          "/v1/configuration" to validateSecretKeyResponse,
          "/v1/transfers" to badRequestResponse
        )
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        service.sendPayment(
          Account(PaymentNetwork.CIRCLE, "1000066041", Account.Capabilities()),
          Account(
            PaymentNetwork.STELLAR,
            "GBG7VGZFH4TU2GS7WL5LMPYFNP64ZFR23XEGAV7GPEEXKWOR2DKCYPCK",
            "test tag",
            Account.Capabilities(PaymentNetwork.CIRCLE, PaymentNetwork.STELLAR)
          ),
          "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
          BigDecimal(1)
        ),
        hashMapOf(
          "/v1/configuration" to validateSecretKeyResponse,
          "/v1/transfers" to badRequestResponse
        )
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        service.sendPayment(
          Account(PaymentNetwork.CIRCLE, "1000066041", Account.Capabilities()),
          Account(
            PaymentNetwork.BANK_WIRE,
            "6c87da10-feb8-484f-822c-2083ed762d25",
            "test@mail.com",
            Account.Capabilities()
          ),
          CircleAsset.fiatUSD(),
          BigDecimal(1)
        ),
        hashMapOf(
          "/v1/configuration" to validateSecretKeyResponse,
          "/v1/payouts" to badRequestResponse
        ),
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        (service as CirclePaymentService).getTransfers("1000066041", null, null, null),
        hashMapOf("/v1/transfers?pageSize=50&walletId=1000066041" to badRequestResponse)
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        (service as CirclePaymentService).getPayouts("1000066041", null, null, null),
        hashMapOf("/v1/payouts?pageSize=50&source=1000066041" to badRequestResponse)
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        (service as CirclePaymentService).getIncomingPayments("1000066041", null, null, null),
        hashMapOf(
          "/v1/configuration" to validateSecretKeyResponse,
          "/v1/payments?pageSize=50" to badRequestResponse
        )
      )
    )
    validateErrHandling(
      ErrorHandlingTestCase(
        service.getAccountPaymentHistory("1000066041", null, null),
        hashMapOf(
          "/v1/configuration" to validateSecretKeyResponse,
          "/v1/transfers?pageSize=50&walletId=1000066041" to badRequestResponse,
          "/v1/payouts?pageSize=50&source=1000066041" to badRequestResponse,
          "/v1/payments?pageSize=50" to badRequestResponse
        )
      )
    )
  }
}
