package org.stellar.anchor.paymentservice.circle

import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.IOException
import java.lang.reflect.Method
import java.math.BigDecimal
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
import org.skyscreamer.jsonassert.JSONAssert
import org.stellar.anchor.exception.HttpException
import org.stellar.anchor.paymentservice.*
import org.stellar.anchor.paymentservice.circle.model.CircleWallet
import org.stellar.anchor.paymentservice.circle.util.CircleDateFormatter
import reactor.core.publisher.Mono
import reactor.netty.ByteBufMono
import reactor.netty.http.client.HttpClientResponse
import shadow.com.google.common.reflect.TypeToken

private class ErrorHandlingTestCase {
  val requestMono: Mono<*>
  var mockResponses: List<MockResponse>? = null
    private set
  var mockResponsesMap: Map<String, MockResponse>? = null
    private set

  constructor(_requestMono: Mono<*>, _mockResponses: List<MockResponse>) {
    this.requestMono = _requestMono
    this.mockResponses = _mockResponses
  }

  constructor(_requestMono: Mono<*>, _mockResponsesMap: Map<String, MockResponse>) {
    this.requestMono = _requestMono
    this.mockResponsesMap = _mockResponsesMap
  }

  private fun getDispatcher(): Dispatcher? {
    if (mockResponsesMap == null) {
      return null
    }

    val dispatcher: Dispatcher =
      object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {

          if (!mockResponsesMap!!.containsKey(request.path)) {
            return MockResponse().setResponseCode(404)
          }

          return mockResponsesMap!![request.path]!!
        }
      }
    return dispatcher
  }

  fun prepareMockWebServer(server: MockWebServer) {
    val dispatcher = getDispatcher()
    if (dispatcher != null) {
      server.dispatcher = dispatcher
    } else if (mockResponses != null) {
      mockResponses!!.forEach { mockResponse -> server.enqueue(mockResponse) }
    }
  }
}

class CirclePaymentServiceTest {
  private lateinit var server: MockWebServer
  private lateinit var service: PaymentService

  @BeforeEach
  @Throws(IOException::class)
  fun setUp() {
    server = MockWebServer()
    server.start()
    service = CirclePaymentService(server.url("").toString(), "<secret-key>")
  }

  @AfterEach
  @Throws(IOException::class)
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun test_private_handleCircleError() {
    // mock objects
    val response = mockk<HttpClientResponse>()
    every { response.status() } returns HttpResponseStatus.BAD_REQUEST
    val bodyBytesMono = mockk<ByteBufMono>()
    every { bodyBytesMono.asString() } returns
      Mono.just("{\"code\":2,\"message\":\"Request body contains unprocessable entity.\"}")

    // access private method
    val handleCircleErrorMethod: Method =
      CirclePaymentService::class.java.getDeclaredMethod(
        "handleCircleError",
        HttpClientResponse::class.java,
        ByteBufMono::class.java
      )
    assert(handleCircleErrorMethod.trySetAccessible())

    // run and test
    val ex =
      assertThrows<HttpException> {
        (handleCircleErrorMethod.invoke(service, response, bodyBytesMono) as Mono<*>).block()
      }
    verify { response.status() }
    verify { bodyBytesMono.asString() }
    assertEquals(HttpException(400, "Request body contains unprocessable entity.", "2"), ex)
  }

  @Test
  fun testCirclePing() {
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
  fun testGetDistributionAccountAddress() {
    val response =
      MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody(
          """{
                        "data":{
                            "payments":{
                                "masterWalletId":"1000066041"
                            }
                        }
                    }"""
        )
    server.enqueue(response)

    var masterWalletId: String? = null
    assertDoesNotThrow { masterWalletId = service.distributionAccountAddress.block() }
    assertEquals("1000066041", masterWalletId)

    val request = server.takeRequest()
    assertEquals("GET", request.method)
    assertEquals("application/json", request.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", request.headers["Authorization"])
    assertThat(request.path, CoreMatchers.endsWith("/v1/configuration"))

    // check if cached version doesn't freeze the thread
    server.enqueue(response)
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
  fun testGetAccount_isNotMainAccount() {
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
    assertEquals(Network.CIRCLE, account?.network)
    assertEquals(Account.Capabilities(Network.CIRCLE, Network.STELLAR), account?.capabilities)
    assertEquals("Treasury Wallet", account?.idTag)
    assertEquals(1, account?.balances?.size)
    assertEquals("29472389929.00", account!!.balances[0].amount)
    assertEquals("circle:USD", account!!.balances[0].currencyName)
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
  fun testGetAccount_isMainAccount() {
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
                                            "masterWalletId":"1000066041"
                                        }
                                    }
                            }"""
                )
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
    assertEquals(Network.CIRCLE, account?.network)
    assertEquals(
      Account.Capabilities(Network.CIRCLE, Network.STELLAR, Network.BANK_WIRE),
      account?.capabilities
    )
    assertNull(account?.idTag)
    assertEquals(1, account?.balances?.size)
    assertEquals("29472389929.00", account!!.balances[0].amount)
    assertEquals("circle:USD", account!!.balances[0].currencyName)
    assertEquals(1, account?.unsettledBalances?.size)
    assertEquals("100.00", account!!.unsettledBalances[0].amount)
    assertEquals("circle:USD", account!!.unsettledBalances[0].currencyName)

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
    assertEquals(Network.CIRCLE, account?.network)
    assertEquals(Account.Capabilities(Network.CIRCLE, Network.STELLAR), account?.capabilities)
    assertEquals("Foo bar", account?.idTag)
    assertEquals(1, account?.balances?.size)
    assertEquals("123.45", account!!.balances[0].amount)
    assertEquals("circle:USD", account!!.balances[0].currencyName)
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
  fun testSendPayment_circleToWire() {
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
                                            "masterWalletId":"1000066041"
                                        }
                                    }
                            }"""
                )
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

    val source = Account(Network.CIRCLE, "1000066041", Account.Capabilities())
    val destination =
      Account(
        Network.BANK_WIRE,
        "6c87da10-feb8-484f-822c-2083ed762d25",
        "test@mail.com",
        Account.Capabilities()
      )
    var payment: Payment? = null
    assertDoesNotThrow {
      payment =
        service.sendPayment(source, destination, "iso4217:USD", BigDecimal.valueOf(0.91)).block()
    }

    assertEquals("c58e2613-a808-4075-956c-e576787afb3b", payment?.id)
    assertEquals(
      Account(
        Network.CIRCLE,
        "1000066041",
        Account.Capabilities(Network.CIRCLE, Network.STELLAR, Network.BANK_WIRE)
      ),
      payment?.sourceAccount
    )
    assertEquals(
      Account(
        Network.BANK_WIRE,
        "6c87da10-feb8-484f-822c-2083ed762d25",
        Account.Capabilities(Network.BANK_WIRE)
      ),
      payment?.destinationAccount
    )
    assertEquals(Balance("0.91", "iso4217:USD"), payment?.balance)
    assertEquals(Payment.Status.PENDING, payment?.status)
    assertNull(payment?.errorCode)

    val wantCreateDate = CircleDateFormatter.stringToDate("2021-11-25T15:43:03.477Z")
    assertEquals(wantCreateDate, payment?.createdAt)
    val wantUpdateDate = CircleDateFormatter.stringToDate("2021-11-25T16:10:01.618Z")
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

    assertEquals(2, server.requestCount)
    val allRequests = arrayOf(server.takeRequest(), server.takeRequest())

    val validateSecretKeyRequest =
      allRequests.find { request -> request.path!! == "/v1/configuration" }!!
    assertEquals("GET", validateSecretKeyRequest.method)
    assertEquals("application/json", validateSecretKeyRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", validateSecretKeyRequest.headers["Authorization"])

    val postPayoutRequest = allRequests.find { request -> request.path!! == "/v1/payouts" }!!
    assertEquals("POST", postPayoutRequest.method)
    assertEquals("application/json", postPayoutRequest.headers["Content-Type"])
    assertEquals("Bearer <secret-key>", postPayoutRequest.headers["Authorization"])
    val gotBody = postPayoutRequest.body.readUtf8()
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
  fun testSendPayment_parameterValidation() {
    // invalid source account network
    var ex =
      assertThrows<HttpException> {
        service
          .sendPayment(
            Account(Network.STELLAR, "123", Account.Capabilities()),
            Account(Network.CIRCLE, "123", Account.Capabilities()),
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
            Account(Network.CIRCLE, "123", Account.Capabilities()),
            Account(Network.BANK_WIRE, "123", "invalidEmail", Account.Capabilities()),
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
            Account(Network.CIRCLE, "123", Account.Capabilities()),
            Account(Network.CIRCLE, "456", Account.Capabilities()),
            "invalidSchema:USD",
            BigDecimal(0)
          )
          .block()
      }
    assertEquals(
      HttpException(400, "the currency to be sent must contain the destination network schema"),
      ex
    )
  }

  @Test
  fun test_private_getTransfers() {
    service.secretKey = "<secret-key>"

    val mockWalletToWalletTransfer =
      """{
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
      }"""

    val mockStellarToWalletTransfer =
      """{
        "id":"7f131f58-a8a0-3dc2-be05-6a015c69de35",
        "source":{
          "type":"blockchain",
          "chain":"XLM"
        },
        "destination":{
          "type":"wallet",
          "id":"1000066041",
          "address":"GAYF33NNNMI2Z6VNRFXQ64D4E4SF77PM46NW3ZUZEEU5X7FCHAZCMHKU",
          "addressTag":"8006436449031889621"
        },
        "amount":{
          "amount":"1.50",
          "currency":"USD"
        },
        "transactionHash":"fb8947c67856d8eb444211c1927d92bcf14abcfb34cdd27fc9e604b15d208fd1",
        "status":"complete",
        "createDate":"2022-02-07T18:02:17.999Z"
       }"""

    val mockWalletToStellarTransfer =
      """{
        "id":"a8997020-3da7-4543-bc4a-5ae8c7ce346d",
        "source":{
          "type":"wallet",
          "id":"1000066041"
        },
        "destination":{
          "type":"blockchain",
          "address":"GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
          "chain":"XLM"
        },
        "amount":{
          "amount":"1.00",
          "currency":"USD"
        },
        "transactionHash":"5239ee055b1083231c6bdaaa921d3e4b3bc090577fbd909815bd5d7fe68091ef",
        "status":"complete",
        "createDate":"2022-02-07T19:50:23.408Z"
       }"""

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
                      $mockWalletToWalletTransfer,
                      $mockStellarToWalletTransfer,
                      $mockWalletToStellarTransfer
                    ]
                  }""".trimIndent()
                )
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    // Let's use reflection to access the private method
    val getTransfersMethod: Method =
      CirclePaymentService::class.java.getDeclaredMethod(
        "getTransfers",
        String::class.java,
        String::class.java,
        String::class.java,
        Integer::class.java
      )
    assert(getTransfersMethod.trySetAccessible())
    @Suppress("UNCHECKED_CAST")
    val getTransfersMono =
      (getTransfersMethod.invoke(service, "1000066041", null, null, null) as Mono<PaymentHistory>)

    var paymentHistory: PaymentHistory? = null
    assertDoesNotThrow { paymentHistory = getTransfersMono.block() }

    val account =
      Account(Network.CIRCLE, "1000066041", Account.Capabilities(Network.CIRCLE, Network.STELLAR))
    val wantPaymentHistory = PaymentHistory(account)
    wantPaymentHistory.beforeCursor = "c58e2613-a808-4075-956c-e576787afb3b"

    val gson = Gson()
    val type = object : TypeToken<Map<String?, *>?>() {}.type

    val p1 = Payment()
    p1.id = "c58e2613-a808-4075-956c-e576787afb3b"
    p1.sourceAccount = account
    p1.destinationAccount =
      Account(Network.CIRCLE, "1000067536", Account.Capabilities(Network.CIRCLE, Network.STELLAR))
    p1.balance = Balance("0.91", "circle:USD")
    p1.status = Payment.Status.PENDING
    p1.createdAt = CircleDateFormatter.stringToDate("2022-01-01T01:01:01.544Z")
    p1.updatedAt = CircleDateFormatter.stringToDate("2022-01-01T01:01:01.544Z")
    p1.originalResponse = gson.fromJson(mockWalletToWalletTransfer, type)
    wantPaymentHistory.payments.add(p1)

    val p2 = Payment()
    p2.id = "7f131f58-a8a0-3dc2-be05-6a015c69de35"
    p2.sourceAccount = Account(Network.STELLAR, null, Account.Capabilities(Network.STELLAR))
    p2.destinationAccount = account
    p2.balance = Balance("1.50", "circle:USD")
    p2.txHash = "fb8947c67856d8eb444211c1927d92bcf14abcfb34cdd27fc9e604b15d208fd1"
    p2.status = Payment.Status.SUCCESSFUL
    p2.createdAt = CircleDateFormatter.stringToDate("2022-02-07T18:02:17.999Z")
    p2.updatedAt = CircleDateFormatter.stringToDate("2022-02-07T18:02:17.999Z")
    p2.originalResponse = gson.fromJson(mockStellarToWalletTransfer, type)
    wantPaymentHistory.payments.add(p2)

    val p3 = Payment()
    p3.id = "a8997020-3da7-4543-bc4a-5ae8c7ce346d"
    p3.sourceAccount = account
    p3.destinationAccount =
      Account(
        Network.STELLAR,
        "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
        Account.Capabilities(Network.STELLAR)
      )
    p3.balance = Balance("1.00", "stellar:USD")
    p3.txHash = "5239ee055b1083231c6bdaaa921d3e4b3bc090577fbd909815bd5d7fe68091ef"
    p3.status = Payment.Status.SUCCESSFUL
    p3.createdAt = CircleDateFormatter.stringToDate("2022-02-07T19:50:23.408Z")
    p3.updatedAt = CircleDateFormatter.stringToDate("2022-02-07T19:50:23.408Z")
    p3.originalResponse = gson.fromJson(mockWalletToStellarTransfer, type)
    wantPaymentHistory.payments.add(p3)

    assertEquals(wantPaymentHistory, paymentHistory)
  }

  @Test
  fun test_private_getTransfers_paginationResponse() {
    service.secretKey = "<secret-key>"

    val mockWalletToWalletTransfer =
      """{
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
      }"""

    val mockWalletToStellarTransfer =
      """{
        "id":"a8997020-3da7-4543-bc4a-5ae8c7ce346d",
        "source":{
          "type":"wallet",
          "id":"1000066041"
        },
        "destination":{
          "type":"blockchain",
          "address":"GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
          "chain":"XLM"
        },
        "amount":{
          "amount":"1.00",
          "currency":"USD"
        },
        "transactionHash":"5239ee055b1083231c6bdaaa921d3e4b3bc090577fbd909815bd5d7fe68091ef",
        "status":"complete",
        "createDate":"2022-02-07T19:50:23.408Z"
       }"""

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
                      $mockWalletToWalletTransfer,
                      $mockWalletToStellarTransfer
                    ]
                  }""".trimIndent()
                )
          }
          return MockResponse().setResponseCode(404)
        }
      }
    server.dispatcher = dispatcher

    // Let's use reflection to access the private method
    val getTransfersMethod: Method =
      CirclePaymentService::class.java.getDeclaredMethod(
        "getTransfers",
        String::class.java,
        String::class.java,
        String::class.java,
        Integer::class.java
      )
    assert(getTransfersMethod.trySetAccessible())
    @Suppress("UNCHECKED_CAST")
    val getTransfersMono =
      (getTransfersMethod.invoke(service, "1000066041", null, null, 2) as Mono<PaymentHistory>)

    var paymentHistory: PaymentHistory? = null
    assertDoesNotThrow { paymentHistory = getTransfersMono.block() }

    val account =
      Account(Network.CIRCLE, "1000066041", Account.Capabilities(Network.CIRCLE, Network.STELLAR))
    val wantPaymentHistory = PaymentHistory(account)
    wantPaymentHistory.beforeCursor = "c58e2613-a808-4075-956c-e576787afb3b"
    wantPaymentHistory.afterCursor = "a8997020-3da7-4543-bc4a-5ae8c7ce346d"

    val gson = Gson()
    val type = object : TypeToken<Map<String?, *>?>() {}.type

    val p1 = Payment()
    p1.id = "c58e2613-a808-4075-956c-e576787afb3b"
    p1.sourceAccount = account
    p1.destinationAccount =
      Account(Network.CIRCLE, "1000067536", Account.Capabilities(Network.CIRCLE, Network.STELLAR))
    p1.balance = Balance("0.91", "circle:USD")
    p1.status = Payment.Status.PENDING
    p1.createdAt = CircleDateFormatter.stringToDate("2022-01-01T01:01:01.544Z")
    p1.updatedAt = CircleDateFormatter.stringToDate("2022-01-01T01:01:01.544Z")
    p1.originalResponse = gson.fromJson(mockWalletToWalletTransfer, type)
    wantPaymentHistory.payments.add(p1)

    val p2 = Payment()
    p2.id = "a8997020-3da7-4543-bc4a-5ae8c7ce346d"
    p2.sourceAccount = account
    p2.destinationAccount =
      Account(
        Network.STELLAR,
        "GAC2OWWDD75GCP4II35UCLYA7JB6LDDZUBZQLYANAVIHIRJAAQBSCL2S",
        Account.Capabilities(Network.STELLAR)
      )
    p2.balance = Balance("1.00", "stellar:USD")
    p2.txHash = "5239ee055b1083231c6bdaaa921d3e4b3bc090577fbd909815bd5d7fe68091ef"
    p2.status = Payment.Status.SUCCESSFUL
    p2.createdAt = CircleDateFormatter.stringToDate("2022-02-07T19:50:23.408Z")
    p2.updatedAt = CircleDateFormatter.stringToDate("2022-02-07T19:50:23.408Z")
    p2.originalResponse = gson.fromJson(mockWalletToStellarTransfer, type)
    wantPaymentHistory.payments.add(p2)

    assertEquals(wantPaymentHistory, paymentHistory)
  }

  @Test
  fun test_private_getTransfers_paginationRequestUri() {
    service.secretKey = "<secret-key>"

    val emptyResponse =
      MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody("""{    
          "data": []
        }""".trimIndent())

    // Let's use reflection to access the private method
    val getTransfersMethod: Method =
      CirclePaymentService::class.java.getDeclaredMethod(
        "getTransfers",
        String::class.java,
        String::class.java,
        String::class.java,
        Integer::class.java
      )
    assert(getTransfersMethod.trySetAccessible())
    var paymentHistory: PaymentHistory? = null

    val account =
      Account(Network.CIRCLE, "1000066041", Account.Capabilities(Network.CIRCLE, Network.STELLAR))

    // GET without pagination: /v1/transfers?pageSize=1&walletId=1000066041
    server.enqueue(emptyResponse)
    @Suppress("UNCHECKED_CAST")
    var getTransfersMono =
      (getTransfersMethod.invoke(service, "1000066041", null, null, 1) as Mono<PaymentHistory>)
    assertDoesNotThrow { paymentHistory = getTransfersMono.block() }

    var wantPaymentHistory = PaymentHistory(account)
    assertEquals(wantPaymentHistory, paymentHistory)

    var wantUri = "/v1/transfers?pageSize=1&walletId=1000066041"
    var request = server.takeRequest()
    assertThat(request.path, CoreMatchers.endsWith(wantUri))
    assertEquals("GET", request.method)
    assertEquals("Bearer <secret-key>", request.headers["Authorization"])

    // GET with before pagination:
    // /v1/transfers?pageSize=1&walletId=1000066041&pageBefore=c58e2613-a808-4075-956c-e576787afb3b
    server.enqueue(emptyResponse)
    @Suppress("UNCHECKED_CAST")
    getTransfersMono =
      (getTransfersMethod.invoke(
        service,
        "1000066041",
        "c58e2613-a808-4075-956c-e576787afb3b",
        null,
        1
      ) as
        Mono<PaymentHistory>)
    assertDoesNotThrow { paymentHistory = getTransfersMono.block() }

    wantPaymentHistory = PaymentHistory(account)
    assertEquals(wantPaymentHistory, paymentHistory)

    wantUri =
      "/v1/transfers?pageSize=1&walletId=1000066041&pageBefore=c58e2613-a808-4075-956c-e576787afb3b"
    request = server.takeRequest()
    assertThat(request.path, CoreMatchers.endsWith(wantUri))
    assertEquals("GET", request.method)
    assertEquals("Bearer <secret-key>", request.headers["Authorization"])

    // GET with after pagination:
    // /v1/transfers?pageSize=1&walletId=1000066041&pageAfter=a8997020-3da7-4543-bc4a-5ae8c7ce346d
    server.enqueue(emptyResponse)
    @Suppress("UNCHECKED_CAST")
    getTransfersMono =
      (getTransfersMethod.invoke(
        service,
        "1000066041",
        null,
        "a8997020-3da7-4543-bc4a-5ae8c7ce346d",
        1
      ) as
        Mono<PaymentHistory>)
    assertDoesNotThrow { paymentHistory = getTransfersMono.block() }

    wantPaymentHistory = PaymentHistory(account)
    assertEquals(wantPaymentHistory, paymentHistory)

    wantUri =
      "/v1/transfers?pageSize=1&walletId=1000066041&pageAfter=a8997020-3da7-4543-bc4a-5ae8c7ce346d"
    request = server.takeRequest()
    assertThat(request.path, CoreMatchers.endsWith(wantUri))
    assertEquals("GET", request.method)
    assertEquals("Bearer <secret-key>", request.headers["Authorization"])

    // GET with after pagination taking precedence over before:
    // /v1/transfers?pageSize=1&walletId=1000066041&pageAfter=a8997020-3da7-4543-bc4a-5ae8c7ce346d
    server.enqueue(emptyResponse)
    @Suppress("UNCHECKED_CAST")
    getTransfersMono =
      (getTransfersMethod.invoke(
        service,
        "1000066041",
        "c58e2613-a808-4075-956c-e576787afb3b",
        "a8997020-3da7-4543-bc4a-5ae8c7ce346d",
        1
      ) as
        Mono<PaymentHistory>)
    assertDoesNotThrow { paymentHistory = getTransfersMono.block() }

    wantPaymentHistory = PaymentHistory(account)
    assertEquals(wantPaymentHistory, paymentHistory)

    wantUri =
      "/v1/transfers?pageSize=1&walletId=1000066041&pageAfter=a8997020-3da7-4543-bc4a-5ae8c7ce346d"
    request = server.takeRequest()
    assertThat(request.path, CoreMatchers.endsWith(wantUri))
    assertEquals("GET", request.method)
    assertEquals("Bearer <secret-key>", request.headers["Authorization"])
  }

  @Test
  fun testSendPayment_circleToCircle() {
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
                                            "masterWalletId":"1000066041"
                                        }
                                    }
                            }"""
                )
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

    val source =
      Account(Network.CIRCLE, "1000066041", Account.Capabilities(Network.CIRCLE, Network.STELLAR))
    val destination =
      Account(Network.CIRCLE, "1000067536", Account.Capabilities(Network.CIRCLE, Network.STELLAR))
    var payment: Payment? = null
    assertDoesNotThrow {
      payment =
        service.sendPayment(source, destination, "circle:USD", BigDecimal.valueOf(0.91)).block()
    }

    assertEquals("c58e2613-a808-4075-956c-e576787afb3b", payment?.id)
    assertEquals(
      Account(
        Network.CIRCLE,
        "1000066041",
        Account.Capabilities(Network.CIRCLE, Network.STELLAR, Network.BANK_WIRE)
      ),
      payment?.sourceAccount
    )
    assertEquals(
      Account(Network.CIRCLE, "1000067536", Account.Capabilities(Network.CIRCLE, Network.STELLAR)),
      payment?.destinationAccount
    )
    assertEquals(Balance("0.91", "circle:USD"), payment?.balance)
    assertEquals(Payment.Status.PENDING, payment?.status)
    assertNull(payment?.errorCode)

    val wantDate = CircleDateFormatter.stringToDate("2022-01-01T01:01:01.544Z")
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
  fun testSendPayment_circleToStellar() {
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
                                            "masterWalletId":"1000066041"
                                        }
                                    }
                            }"""
                )
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

    val source = Account(Network.CIRCLE, "1000066041", Account.Capabilities())
    val destination =
      Account(
        Network.STELLAR,
        "GBG7VGZFH4TU2GS7WL5LMPYFNP64ZFR23XEGAV7GPEEXKWOR2DKCYPCK",
        "test tag",
        Account.Capabilities()
      )
    var payment: Payment? = null
    assertDoesNotThrow {
      payment =
        service.sendPayment(source, destination, "stellar:USD", BigDecimal.valueOf(0.91)).block()
    }

    assertEquals("c58e2613-a808-4075-956c-e576787afb3b", payment?.id)
    assertEquals(
      Account(
        Network.CIRCLE,
        "1000066041",
        Account.Capabilities(Network.CIRCLE, Network.STELLAR, Network.BANK_WIRE)
      ),
      payment?.sourceAccount
    )
    assertEquals(
      Account(
        Network.STELLAR,
        "GBG7VGZFH4TU2GS7WL5LMPYFNP64ZFR23XEGAV7GPEEXKWOR2DKCYPCK",
        "test tag",
        Account.Capabilities(Network.STELLAR)
      ),
      payment?.destinationAccount
    )
    assertEquals(Balance("0.91", "stellar:USD"), payment?.balance)
    assertEquals(Payment.Status.PENDING, payment?.status)
    assertNull(payment?.errorCode)

    val wantDate = CircleDateFormatter.stringToDate("2022-01-01T01:01:01.544Z")
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
  fun testErrorHandling() {
    val badRequestResponse =
      MockResponse()
        .setResponseCode(400)
        .addHeader("Content-Type", "application/json")
        .setBody("{\"code\":2,\"message\":\"Request body contains unprocessable entity.\"}")
    val validateSecretKeyResponse =
      MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody(
          """{
                        "data":{
                            "payments":{
                                "masterWalletId":"1000066041"
                            }
                        }
                    }"""
        )
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

    // Access private method getMainAccountBalances
    val getMerchantAccountUnsettledBalancesMethod: Method =
      CirclePaymentService::class.java.getDeclaredMethod("getMerchantAccountUnsettledBalances")
    assert(getMerchantAccountUnsettledBalancesMethod.trySetAccessible())

    // Access private method getCircleWallet
    val getCircleWalletMethod: Method =
      CirclePaymentService::class.java.getDeclaredMethod("getCircleWallet", String::class.java)
    assert(getCircleWalletMethod.trySetAccessible())

    listOf(
      // --- tests with sync/serial requests ---
      ErrorHandlingTestCase(service.ping(), listOf(badRequestResponse)),
      ErrorHandlingTestCase(service.distributionAccountAddress, listOf(badRequestResponse)),
      ErrorHandlingTestCase(service.getAccount("random_id"), listOf(badRequestResponse)),
      ErrorHandlingTestCase(
        service.getAccount("random_id"),
        listOf(validateSecretKeyResponse, badRequestResponse)
      ),
      ErrorHandlingTestCase(
        getMerchantAccountUnsettledBalancesMethod.invoke(service) as Mono<*>,
        listOf(badRequestResponse)
      ),
      ErrorHandlingTestCase(service.createAccount(null), listOf(badRequestResponse)),
      ErrorHandlingTestCase(
        getCircleWalletMethod.invoke(service, "random_id") as Mono<*>,
        listOf(badRequestResponse)
      ),
      // --- tests with async/parallel requests ---
      // ATTENTION, make sure to run parallel tests at the end, if you try to run a serial test
      // after a parallel
      // test, the server dispatcher will throw an exception.
      ErrorHandlingTestCase(
        service.getAccount("1000066041"),
        hashMapOf(
          "/v1/configuration" to validateSecretKeyResponse,
          "/v1/businessAccount/balances" to mainAccountResponse,
          "/v1/wallets/1000066041" to badRequestResponse
        )
      ),
      ErrorHandlingTestCase(
        service.sendPayment(
          Account(Network.CIRCLE, "1000066041", Account.Capabilities()),
          Account(Network.CIRCLE, "1000067536", Account.Capabilities()),
          "circle:USD",
          BigDecimal.valueOf(1)
        ),
        hashMapOf(
          "/v1/configuration" to validateSecretKeyResponse,
          "/v1/transfers" to badRequestResponse
        )
      ),
      ErrorHandlingTestCase(
        service.sendPayment(
          Account(Network.CIRCLE, "1000066041", Account.Capabilities()),
          Account(
            Network.STELLAR,
            "GBG7VGZFH4TU2GS7WL5LMPYFNP64ZFR23XEGAV7GPEEXKWOR2DKCYPCK",
            "test tag",
            Account.Capabilities(Network.CIRCLE, Network.STELLAR)
          ),
          "stellar:USD",
          BigDecimal(1)
        ),
        hashMapOf(
          "/v1/configuration" to validateSecretKeyResponse,
          "/v1/transfers" to badRequestResponse
        )
      ),
      ErrorHandlingTestCase(
        service.sendPayment(
          Account(Network.CIRCLE, "1000066041", Account.Capabilities()),
          Account(
            Network.BANK_WIRE,
            "6c87da10-feb8-484f-822c-2083ed762d25",
            "test@mail.com",
            Account.Capabilities()
          ),
          "iso4217:USD",
          BigDecimal(1)
        ),
        hashMapOf(
          "/v1/configuration" to validateSecretKeyResponse,
          "/v1/payouts" to badRequestResponse
        )
      ),
    )
      .forEach { testCase ->
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
  }
}
