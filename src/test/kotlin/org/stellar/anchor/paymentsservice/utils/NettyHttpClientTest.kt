package org.stellar.anchor.paymentsservice.utils


internal class NettyHttpClientTest {
//    private lateinit var server: MockWebServer
//    private lateinit var httpClient: HttpClient
//
//    @BeforeEach
//    @Throws(IOException::class)
//    fun setUp() {
//        server = MockWebServer()
//        server.start()
//        val baseUrl = server.url("").toString()
//        httpClient = NettyHttpClient.withBaseUrl(baseUrl)
//    }
//
//    @AfterEach
//    fun tearDown() {
//        server.shutdown()
//    }
//
//    @Test
//    fun testHttpClient_readTimeoutResponse() {
//        val timeoutResponse = MockResponse()
//            .setBody("{\"foo\": \"bar\"}")
//            .setBodyDelay(16, TimeUnit.SECONDS)
//        server.enqueue(timeoutResponse)
//
//        assertThrows<ReadTimeoutException> { httpClient.get().responseContent().aggregate().asString().block() }
//    }
//
//    @Test
//    @Timeout(value = 5500, unit = TimeUnit.MILLISECONDS)
//    fun testHttpClient_connectionTimeout() {
//        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.SHUTDOWN_SERVER_AFTER_RESPONSE))
//
//        // throws from the second time onwards because of the option SocketPolicy.SHUTDOWN_SERVER_AFTER_RESPONSE
//        assertDoesNotThrow { httpClient.get().responseContent().aggregate().asString().block() }
//        val ex = assertThrows<RuntimeException> { httpClient.get().responseContent().aggregate().asString().block() }
//        assertNotEquals(-1, ExceptionUtils.indexOfThrowable(ex, ConnectException::class.java))
//    }
}
