package org.stellar.anchor.platform.config

import kotlin.test.assertEquals
import org.junit.jupiter.api.*
import org.springframework.validation.BindException
import org.springframework.validation.ValidationUtils

open class Sep38ConfigTest {
  @Test
  fun testSep38ConfigValid() {
    val sep38Config = PropertySep38Config()
    sep38Config.enabled = true

    val errors = BindException(sep38Config, "sep38Config")
    ValidationUtils.invokeValidator(sep38Config, sep38Config, errors)
    assertEquals(0, errors.errorCount)
  }
}
