package org.stellar.anchor.platform.config

import kotlin.test.assertEquals
import org.junit.jupiter.api.*
import org.springframework.validation.BindException
import org.springframework.validation.ValidationUtils

open class Sep12ConfigTest {
  @Test
  fun testSep12ConfigValid() {
    val config = CallbackApiConfig(PropertySecretConfig())
    config.baseUrl = "https://localhost:8081"
    val sep12Config = PropertySep12Config(config)
    sep12Config.enabled = true

    val errors = BindException(sep12Config, "sep12Config")
    ValidationUtils.invokeValidator(sep12Config, sep12Config, errors)
    assertEquals(0, errors.errorCount)
  }
}
