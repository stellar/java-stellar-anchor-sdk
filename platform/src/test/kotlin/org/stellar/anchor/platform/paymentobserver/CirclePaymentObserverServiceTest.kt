package org.stellar.anchor.platform.paymentobserver

import io.mockk.*
import io.mockk.impl.annotations.MockK
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.config.CirclePaymentObserverConfig
import org.stellar.anchor.exception.UnprocessableEntityException

class CirclePaymentObserverServiceTest {
  @MockK private lateinit var httpClient: OkHttpClient
  @MockK private lateinit var circlePaymentObserverConfig: CirclePaymentObserverConfig
  @MockK private lateinit var circlePaymentObserverService: CirclePaymentObserverService

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true)

    every { circlePaymentObserverConfig.stellarNetwork } returns "TESTNET"
    circlePaymentObserverService =
      CirclePaymentObserverService(httpClient, circlePaymentObserverConfig)
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
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
    ex = assertThrows { circlePaymentObserverService.handleCircleNotification(unsupportedNotification) }
    assertEquals("Not handling notification of unsupported type \"ABC\".", ex.message)
    assertInstanceOf(UnprocessableEntityException::class.java, ex)
  }
}
