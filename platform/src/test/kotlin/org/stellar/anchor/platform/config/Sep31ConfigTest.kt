package org.stellar.anchor.platform.config

import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.jupiter.api.*
import org.springframework.validation.BindException
import org.springframework.validation.ValidationUtils
import org.stellar.anchor.config.Sep31Config.DepositInfoGeneratorType.CIRCLE

open class Sep31ConfigTest {
  @Test
  fun testSep31WithCircleConfig() {
    val circleConfig = PropertyCircleConfig()
    circleConfig.setCircleUrl("https://api-sandbox.circle.com")
    val sep31Config = PropertySep31Config(circleConfig)
    sep31Config.setDepositInfoGeneratorType(CIRCLE)

    var errors = BindException(sep31Config, "sep31Config")
    ValidationUtils.invokeValidator(sep31Config, sep31Config, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "badConfig-circleConfig") }

    errors = BindException(circleConfig, "circleConfig")
    ValidationUtils.invokeValidator(circleConfig, circleConfig, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "empty-apiKey") }
  }
}
