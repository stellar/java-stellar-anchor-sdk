package org.stellar.anchor.paymentservice.circle

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.netty.handler.codec.http.HttpResponseStatus
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
import java.io.IOException
import java.lang.reflect.Method
import java.math.BigDecimal


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

        val dispatcher: Dispatcher = object : Dispatcher() {
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

class CirclePaymentsServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var service: PaymentsService

    @BeforeEach
    @Throws(IOException::class)
    fun setUp() {
        server = MockWebServer()
        server.start()
        service = CirclePaymentsService()
        service.url = server.url("").toString()
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
        every { bodyBytesMono.asString() } returns Mono.just("{\"code\":2,\"message\":\"Request body contains unprocessable entity.\"}")

        // access private method
        val handleCircleErrorMethod: Method = CirclePaymentsService::class.java.getDeclaredMethod(
            "handleCircleError",
            HttpClientResponse::class.java,
            ByteBufMono::class.java
        )
        assert(handleCircleErrorMethod.trySetAccessible())

        // run and test
        val ex = assertThrows<HttpException> {
            (handleCircleErrorMethod.invoke(
                service,
                response,
                bodyBytesMono
            ) as Mono<*>).block()
        }
        verify { response.status() }
        verify { bodyBytesMono.asString() }
        assertEquals(HttpException(400, "Request body contains unprocessable entity.", "2"), ex)
    }

    @Test
    fun testCirclePing() {
        val response = MockResponse()
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
    fun testValidateSecretKey() {
        service.secretKey = "<secret-key>"
        val response = MockResponse()
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

        assertDoesNotThrow { service.validateSecretKey().block() }

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("Bearer <secret-key>", request.headers["Authorization"])
        assertThat(request.path, CoreMatchers.endsWith("/v1/configuration"))
    }

    @Test
    fun testGetDistributionAccountAddress() {
        service.secretKey = "<secret-key>"
        val response = MockResponse()
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
        service.secretKey = "<secret-key>"
        val response = MockResponse()
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
            CirclePaymentsService::class.java.getDeclaredMethod("getMerchantAccountUnsettledBalances")
        assert(getMerchantAccountUnsettledBalancesMethod.trySetAccessible())
        @Suppress("UNCHECKED_CAST")
        val unsettledBalancesMono = (getMerchantAccountUnsettledBalancesMethod.invoke(service) as Mono<List<Balance>>)

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
        service.secretKey = "<secret-key>"
        val response = MockResponse()
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
            CirclePaymentsService::class.java.getDeclaredMethod("getCircleWallet", String::class.java)
        assert(getCircleWalletMethod.trySetAccessible())

        var circleWallet: CircleWallet? = null
        assertDoesNotThrow {
            circleWallet = (getCircleWalletMethod.invoke(service, "1000223064") as Mono<CircleWallet>).block()
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
        service.secretKey = "<secret-key>"
        val dispatcher: Dispatcher = object : Dispatcher() {
            @Throws(InterruptedException::class)
            override fun dispatch(request: RecordedRequest): MockResponse {
                when (request.path) {
                    "/v1/configuration" -> return MockResponse()
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

                    "/v1/wallets/1000223064" -> return MockResponse()
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

        val validateSecretKeyRequest = allRequests.find { request -> request.path!! == "/v1/configuration" }!!
        assertEquals("GET", validateSecretKeyRequest.method)
        assertEquals("application/json", validateSecretKeyRequest.headers["Content-Type"])
        assertEquals("Bearer <secret-key>", validateSecretKeyRequest.headers["Authorization"])

        val getAccountRequest = allRequests.find { request -> request.path!! == "/v1/wallets/1000223064" }!!
        assertEquals("GET", getAccountRequest.method)
        assertEquals("application/json", getAccountRequest.headers["Content-Type"])
        assertEquals("Bearer <secret-key>", getAccountRequest.headers["Authorization"])
    }

    @Test
    fun testGetAccount_isMainAccount() {
        service.secretKey = "<secret-key>"
        val dispatcher: Dispatcher = object : Dispatcher() {
            @Throws(InterruptedException::class)
            override fun dispatch(request: RecordedRequest): MockResponse {
                when (request.path) {
                    "/v1/configuration" -> return MockResponse()
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

                    "/v1/businessAccount/balances" -> return MockResponse()
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

                    "/v1/wallets/1000066041" -> return MockResponse()
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
        assertEquals(Account.Capabilities(Network.CIRCLE, Network.STELLAR), account?.capabilities)
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

        val mainAccountRequest = parallelRequests.find { request -> request.path!! == "/v1/businessAccount/balances" }!!
        assertEquals("GET", mainAccountRequest.method)
        assertEquals("application/json", mainAccountRequest.headers["Content-Type"])
        assertEquals("Bearer <secret-key>", mainAccountRequest.headers["Authorization"])

        val getAccountRequest = parallelRequests.find { request -> request.path!! == "/v1/wallets/1000066041" }!!
        assertEquals("GET", getAccountRequest.method)
        assertEquals("application/json", getAccountRequest.headers["Content-Type"])
        assertEquals("Bearer <secret-key>", getAccountRequest.headers["Authorization"])
    }

    @Test
    fun test_createAccount() {
        service.secretKey = "<secret-key>"
        val response = MockResponse()
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
    fun testSendPayment_parameterValidation() {
        // invalid source account network
        var ex = assertThrows<HttpException> {
            service.sendPayment(
                Account(Network.STELLAR, "123", Account.Capabilities(Network.STELLAR)),
                Account(Network.CIRCLE, "123", Account.Capabilities(Network.CIRCLE, Network.STELLAR)),
                "",
                BigDecimal(0)
            ).block()
        }
        assertEquals(HttpException(400, "the only supported network for the source account is circle"), ex)

        // invalid currency name schema
        ex = assertThrows {
            service.sendPayment(
                Account(Network.CIRCLE, "123", Account.Capabilities(Network.CIRCLE, Network.STELLAR)),
                Account(Network.CIRCLE, "456", Account.Capabilities(Network.CIRCLE, Network.STELLAR)),
                "invalidSchema:USD",
                BigDecimal(0)
            ).block()
        }
        assertEquals(HttpException(400, "the currency to be sent must contain the destination network schema"), ex)
    }

    @Test
    fun testSendPayment_circleToCircle() {
        service.secretKey = "<secret-key>"
        val dispatcher: Dispatcher = object : Dispatcher() {
            @Throws(InterruptedException::class)
            override fun dispatch(request: RecordedRequest): MockResponse {
                when (request.path) {
                    "/v1/configuration" -> return MockResponse()
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

                    "/v1/transfers" -> return MockResponse()
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
                                "status":"complete",
                                "createDate":"2022-01-01T01:01:01.544Z"
                            }
                        }""".trimIndent()
                        )
                }
                return MockResponse().setResponseCode(404)
            }
        }
        server.dispatcher = dispatcher

        val source = Account(Network.CIRCLE, "1000066041", Account.Capabilities(Network.CIRCLE, Network.STELLAR))
        val destination = Account(Network.CIRCLE, "1000067536", Account.Capabilities(Network.CIRCLE, Network.STELLAR))
        var payment: Payment? = null
        assertDoesNotThrow { payment = service.sendPayment(source, destination, "circle:USD", BigDecimal.valueOf(0.91)).block() }

        assertEquals("c58e2613-a808-4075-956c-e576787afb3b", payment?.id)
        assertEquals(Account(Network.CIRCLE, "1000066041", Account.Capabilities(Network.CIRCLE, Network.STELLAR)), payment?.sourceAccount)
        assertEquals(Account(Network.CIRCLE, "1000067536", Account.Capabilities(Network.CIRCLE, Network.STELLAR)), payment?.destinationAccount)
        assertEquals(Balance("0.91", "circle:USD"), payment?.balance)
        assertEquals(Payment.Status.SUCCESSFUL, payment?.status)
        assertNull(payment?.errorCode)

        val wantDate = CircleDateFormatter.stringToDate("2022-01-01T01:01:01.544Z");
        assertEquals(wantDate, payment?.createdAt)
        assertEquals(wantDate, payment?.updatedAt)

        val wantOriginalResponse = hashMapOf<String, Any>(
            "id" to "c58e2613-a808-4075-956c-e576787afb3b",
            "source" to hashMapOf<String, Any>(
                "id" to "1000066041",
                "type" to "wallet"
            ),
            "destination" to hashMapOf<String, Any>(
                "id" to "1000067536",
                "type" to "wallet",
            ),
            "amount" to hashMapOf<String, Any>(
                "amount" to "0.91",
                "currency" to "USD",
            ),
            "status" to "complete",
            "createDate" to "2022-01-01T01:01:01.544Z",
        )
        assertEquals(wantOriginalResponse, payment?.originalResponse)

        assertEquals(1, server.requestCount)
        val allRequests = arrayOf(server.takeRequest())

        val postTransferRequest = allRequests.find { request -> request.path!! == "/v1/transfers" }!!
        assertEquals("POST", postTransferRequest.method)
        assertEquals("application/json", postTransferRequest.headers["Content-Type"])
        assertEquals("Bearer <secret-key>", postTransferRequest.headers["Authorization"])
        val gotBody = postTransferRequest.body.readUtf8()
        val wantBody = """{
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
        JSONAssert.assertEquals(wantBody, gotBody, true)
    }

    @Test
    fun testSendPayment_circleToStellar() {
        service.secretKey = "<secret-key>"
        val dispatcher: Dispatcher = object : Dispatcher() {
            @Throws(InterruptedException::class)
            override fun dispatch(request: RecordedRequest): MockResponse {
                when (request.path) {
                    "/v1/configuration" -> return MockResponse()
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

                    "/v1/transfers" -> return MockResponse()
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
                                "status":"complete",
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
            Account(Network.STELLAR, "GBG7VGZFH4TU2GS7WL5LMPYFNP64ZFR23XEGAV7GPEEXKWOR2DKCYPCK", "test tag", Account.Capabilities())
        var payment: Payment? = null
        assertDoesNotThrow { payment = service.sendPayment(source, destination, "stellar:USD", BigDecimal.valueOf(0.91)).block() }

        assertEquals("c58e2613-a808-4075-956c-e576787afb3b", payment?.id)
        assertEquals(Account(Network.CIRCLE, "1000066041", Account.Capabilities(Network.CIRCLE, Network.STELLAR)), payment?.sourceAccount)
        assertEquals(
            Account(
                Network.STELLAR,
                "GBG7VGZFH4TU2GS7WL5LMPYFNP64ZFR23XEGAV7GPEEXKWOR2DKCYPCK",
                "test tag",
                Account.Capabilities(Network.CIRCLE, Network.STELLAR)
            ), payment?.destinationAccount
        )
        assertEquals(Balance("0.91", "circle:USD"), payment?.balance)
        assertEquals(Payment.Status.SUCCESSFUL, payment?.status)
        assertNull(payment?.errorCode)

        val wantDate = CircleDateFormatter.stringToDate("2022-01-01T01:01:01.544Z");
        assertEquals(wantDate, payment?.createdAt)
        assertEquals(wantDate, payment?.updatedAt)

        val wantOriginalResponse = hashMapOf<String, Any>(
            "id" to "c58e2613-a808-4075-956c-e576787afb3b",
            "source" to hashMapOf<String, Any>(
                "id" to "1000066041",
                "type" to "wallet",
            ),
            "destination" to hashMapOf<String, Any>(
                "address" to "GBG7VGZFH4TU2GS7WL5LMPYFNP64ZFR23XEGAV7GPEEXKWOR2DKCYPCK",
                "addressTag" to "test tag",
                "type" to "blockchain",
                "chain" to "XLM",
            ),
            "amount" to hashMapOf<String, Any>(
                "amount" to "0.91",
                "currency" to "USD",
            ),
            "status" to "complete",
            "createDate" to "2022-01-01T01:01:01.544Z",
        )
        assertEquals(wantOriginalResponse, payment?.originalResponse)

        assertEquals(1, server.requestCount)
        val allRequests = arrayOf(server.takeRequest())

        val postTransferRequest = allRequests.find { request -> request.path!! == "/v1/transfers" }!!
        assertEquals("POST", postTransferRequest.method)
        assertEquals("application/json", postTransferRequest.headers["Content-Type"])
        assertEquals("Bearer <secret-key>", postTransferRequest.headers["Authorization"])
        val gotBody = postTransferRequest.body.readUtf8()
        val wantBody = """{
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
        JSONAssert.assertEquals(wantBody, gotBody, true)
    }

    @Test
    fun testErrorHandling() {
        val badRequestResponse = MockResponse()
            .setResponseCode(400)
            .addHeader("Content-Type", "application/json")
            .setBody("{\"code\":2,\"message\":\"Request body contains unprocessable entity.\"}")
        val validateSecretKeyResponse = MockResponse()
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
        val mainAccountResponse = MockResponse()
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
            CirclePaymentsService::class.java.getDeclaredMethod("getMerchantAccountUnsettledBalances")
        assert(getMerchantAccountUnsettledBalancesMethod.trySetAccessible())

        // Access private method getCircleWallet
        val getCircleWalletMethod: Method =
            CirclePaymentsService::class.java.getDeclaredMethod("getCircleWallet", String::class.java)
        assert(getCircleWalletMethod.trySetAccessible())

        listOf(
            // --- tests with sync/serial requests ---
            ErrorHandlingTestCase(service.ping(), listOf(badRequestResponse)),
            ErrorHandlingTestCase(service.validateSecretKey(), listOf(badRequestResponse)),
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
            // ATTENTION, make sure to run parallel tests at the end, if you try to run a serial test after a parallel
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
                    BigDecimal.valueOf(1)
                ),
                hashMapOf(
                    "/v1/configuration" to validateSecretKeyResponse,
                    "/v1/transfers" to badRequestResponse
                )
            ),
        ).forEach { testCase ->
            // run project reactor synchronously
            testCase.prepareMockWebServer(server)
            var request = testCase.requestMono
            val thrown = assertThrows<HttpException> { request.block() }
            assertEquals(HttpException(400, "Request body contains unprocessable entity.", "2"), thrown)

            // run project reactor asynchronously
            var didRunAsyncTask = false
            testCase.prepareMockWebServer(server)
            request = testCase.requestMono.onErrorResume { ex ->
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
