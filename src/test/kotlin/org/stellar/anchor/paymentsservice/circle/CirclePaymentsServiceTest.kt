package org.stellar.anchor.paymentsservice.circle

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
import org.stellar.anchor.exception.HttpException
import org.stellar.anchor.paymentsservice.*
import reactor.core.publisher.Mono
import reactor.netty.ByteBufMono
import reactor.netty.http.client.HttpClientResponse
import java.io.IOException
import java.lang.reflect.Method


private class ErrorHandlingTestCase(_requestMono: Mono<*>, _mockResponses: List<MockResponse>) {
    val requestMono: Mono<*>
    val mockResponses: List<MockResponse>

    init {
        this.requestMono = _requestMono
        this.mockResponses = _mockResponses
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
        assertEquals(AccountLevel.DEFAULT, account?.level)
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
        assertEquals(AccountLevel.DISTRIBUTION, account?.level)
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
        assertEquals(AccountLevel.DEFAULT, account?.level)
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
            ErrorHandlingTestCase(service.ping(), listOf(badRequestResponse)),
            ErrorHandlingTestCase(service.validateSecretKey(), listOf(badRequestResponse)),
            ErrorHandlingTestCase(service.distributionAccountAddress, listOf(badRequestResponse)),
            ErrorHandlingTestCase(service.getAccount("random_id"), listOf(badRequestResponse)),
            ErrorHandlingTestCase(
                service.getAccount("random_id"),
                listOf(validateSecretKeyResponse, badRequestResponse)
            ),
            ErrorHandlingTestCase(
                service.getAccount("1000066041"),
                listOf(validateSecretKeyResponse, mainAccountResponse, badRequestResponse)
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
        ).forEach { testCase ->
            // sync tests
            testCase.mockResponses.forEach { mockResponse -> server.enqueue(mockResponse) }
            var request = testCase.requestMono
            val thrown = assertThrows<HttpException> { request.block() }
            assertEquals(HttpException(400, "Request body contains unprocessable entity.", "2"), thrown)

            // async tests
            var didRunAsyncTask = false
            testCase.mockResponses.forEach { mockResponse -> server.enqueue(mockResponse) }
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
