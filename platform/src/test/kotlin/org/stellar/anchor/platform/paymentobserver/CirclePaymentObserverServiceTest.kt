package org.stellar.anchor.platform.paymentobserver

import io.mockk.*
import io.mockk.impl.annotations.MockK
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.config.CirclePaymentObserverConfig
import org.stellar.anchor.util.Log
import kotlin.test.assertEquals


class CirclePaymentObserverServiceTest {
  @MockK private lateinit var httpClient: OkHttpClient;
  @MockK private lateinit var circlePaymentObserverConfig: CirclePaymentObserverConfig;
  @MockK private lateinit var circlePaymentObserverService: CirclePaymentObserverService;

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true)

    every { circlePaymentObserverConfig.stellarNetwork } returns "TESTNET"
    circlePaymentObserverService = CirclePaymentObserverService(httpClient, circlePaymentObserverConfig)
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun test_handleCircleNotification_ignoreUnsupportedType() {
    // Mock Log.warn
    val messageSlot = slot<String>()
    mockkStatic(Log::class)
    every { Log.warn(capture(messageSlot)) } just Runs

    // Empty type is ignored
    var dummyNotification = mapOf("foo" to "bar")
    circlePaymentObserverService.handleCircleNotification(dummyNotification)
    assertEquals("Not handling notification of unsupported type \"\".", messageSlot.captured)

    // Unsupported type is ignored
    dummyNotification = mapOf("Type" to "ABC")
    circlePaymentObserverService.handleCircleNotification(dummyNotification)
    assertEquals("Not handling notification of unsupported type \"ABC\".", messageSlot.captured)
  }
}