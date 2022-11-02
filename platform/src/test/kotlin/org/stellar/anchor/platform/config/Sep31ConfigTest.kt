package org.stellar.anchor.platform.config

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.validation.BindException
import org.springframework.validation.ValidationUtils
import org.stellar.anchor.config.Sep31Config.DepositInfoGeneratorType.API

open class Sep31ConfigTest {
  @Test
  fun testSep31Valid() {
    val callbackApiConfig = CallbackApiConfig(PropertySecretConfig())
    callbackApiConfig.baseUrl = "http://localhost:8080"

    val sep31Config = PropertySep31Config(callbackApiConfig)
    sep31Config.depositInfoGeneratorType = API

    val errors = BindException(sep31Config, "sep31Config")
    ValidationUtils.invokeValidator(sep31Config, sep31Config, errors)
    assertEquals(0, errors.errorCount)
  }
}
