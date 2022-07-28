package org.stellar.anchor.platform.config

import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.jupiter.api.*
import org.springframework.validation.BindException
import org.springframework.validation.ValidationUtils

open class Sep12ConfigTest {
  @Test
  fun testSep12ConfigValid() {
    val sep12Config = PropertySep12Config()
    sep12Config.enabled = true
    sep12Config.customerIntegrationEndPoint = "https://localhost:8081"

    val errors = BindException(sep12Config, "sep12Config")
    ValidationUtils.invokeValidator(sep12Config, sep12Config, errors)
    assertEquals(0, errors.errorCount)
  }

  @Test
  fun testSep12ConfigBadFile() {
    val sep12Config = PropertySep12Config()
    sep12Config.enabled = true
    val errors = BindException(sep12Config, "sep12Config")
    ValidationUtils.invokeValidator(sep12Config, sep12Config, errors)
    assertEquals(2, errors.errorCount)
    errors.message?.let { assertContains(it, "empty-customerIntegrationEndPoint") }
    errors.message?.let { assertContains(it, "invalidUrl-customerIntegrationEndPoint") }
  }
}
