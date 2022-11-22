package org.stellar.anchor.platform.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.stellar.anchor.platform.config.PaymentObserverConfig.PaymentObserverType.STELLAR
import org.stellar.anchor.platform.config.PaymentObserverConfig.StellarPaymentObserverConfig

class PaymentObserverConfigTest {
  lateinit var config: PaymentObserverConfig
  lateinit var errors: Errors
  lateinit var stellarConfig: StellarPaymentObserverConfig

  @BeforeEach
  fun setUp() {
    config = PaymentObserverConfig()
    errors = BindException(config, "config")
    stellarConfig = StellarPaymentObserverConfig()
  }

  @Test
  fun `test stellar payment observer config`() {
    config.type = STELLAR
    stellarConfig = StellarPaymentObserverConfig(90, 5, 5, 5, 300, 5, 300)
    config.setStellar(stellarConfig)
    config.validateStellar(config, errors)
    assertEquals(0, errors.errorCount)
  }

  @ParameterizedTest
  @CsvSource(
    value =
      [
        "90,5,5,5,300,5,300,0,no-error",
        "0,5,5,5,300,5,300,1,invalid-payment-observer-silence-check-interval",
        "90,0,5,5,300,5,300,1,invalid-payment-observer-stellar-silence-timeout",
        "90,5,0,5,300,5,300,1,invalid-payment-observer-stellar-silence-timeout-retries",
        "90,5,5,0,300,5,300,1,invalid-payment-observer-stellar-initial-stream-backoff-time",
        "90,5,5,5,0,5,300,1,invalid-payment-observer-stellar-max-stream-backoff-time",
        "90,5,5,5,300,0,300,1,invalid-payment-observer-stellar-initial-event-backoff-time",
        "90,5,5,5,300,5,0,1,invalid-payment-observer-stellar-max-event-backoff-time"]
  )
  fun `test invalid stellar config`(
    p0: String,
    p1: String,
    p2: String,
    p3: String,
    p4: String,
    p5: String,
    p6: String,
    errorCount: String,
    errorCode: String
  ) {
    config.type = STELLAR
    config.setStellar(
      StellarPaymentObserverConfig(
        p0.toInt(),
        p1.toInt(),
        p2.toInt(),
        p3.toInt(),
        p4.toInt(),
        p5.toInt(),
        p6.toInt()
      )
    )
    config.validateStellar(config, errors)
    assertEquals(errorCount.toInt(), errors.errorCount)
    if (errors.errorCount > 0) {
      assertEquals(errorCode, errors.allErrors.get(0).code)
    }
  }

  fun `test empty stellar payment observer config`() {
    config.type = STELLAR
    config.setStellar(null)
    config.validateStellar(config, errors)
    assertEquals(1, errors.errorCount)
    assertEquals("empty-payment-observer-stellar", errors.allErrors.get(0).code)
  }
}
