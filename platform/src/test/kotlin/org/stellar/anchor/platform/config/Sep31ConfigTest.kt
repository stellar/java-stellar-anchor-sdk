package org.stellar.anchor.platform.config

import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.jupiter.api.*
import org.springframework.validation.BindException
import org.springframework.validation.ValidationUtils
import org.stellar.anchor.config.SecretConfig
import org.stellar.anchor.config.Sep31Config.DepositInfoGeneratorType.CIRCLE

open class Sep31ConfigTest {
  @Test
  fun testSep31Valid() {
    val circleConfig = PropertyCircleConfig()
    circleConfig.circleUrl = "https://api-sandbox.circle.com"
    circleConfig.apiKey = "apikey"
    val callbackApiConfig = CallbackApiConfig(PropertySecretConfig())
    callbackApiConfig.baseUrl = "http://localhost:8080"

    val sep31Config = PropertySep31Config(circleConfig, callbackApiConfig)
    sep31Config.depositInfoGeneratorType = CIRCLE

    val errors = BindException(sep31Config, "sep31Config")
    ValidationUtils.invokeValidator(sep31Config, sep31Config, errors)
    assertEquals(0, errors.errorCount)

    val circleErrors = circleConfig.validate()
    assertEquals(0, circleErrors.errorCount)
  }

  @Test
  fun testSep31BadCircleConfig() {
    val circleConfig = PropertyCircleConfig()
    circleConfig.circleUrl = "https://api-sandbox.circle.com"
    val callbackApiConfig = CallbackApiConfig(PropertySecretConfig())
    callbackApiConfig.baseUrl = "http://localhost:8080"

    val sep31Config = PropertySep31Config(circleConfig, callbackApiConfig)
    sep31Config.enabled = true
    sep31Config.depositInfoGeneratorType = CIRCLE

    val errors = BindException(sep31Config, "sep31Config")
    ValidationUtils.invokeValidator(sep31Config, sep31Config, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "badConfig-circle") }

    val circleErrors = circleConfig.validate()
    assertEquals(1, circleErrors.errorCount)
    circleErrors.message?.let { assertContains(it, "empty-apiKey") }
  }
}
