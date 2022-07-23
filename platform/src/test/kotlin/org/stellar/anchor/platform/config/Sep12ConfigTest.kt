package org.stellar.anchor.platform.config

import org.stellar.anchor.config.Sep31Config.DepositInfoGeneratorType.CIRCLE

import org.junit.jupiter.api.*
import org.springframework.validation.BindException
import org.springframework.validation.ValidationUtils
import kotlin.test.assertContains
import kotlin.test.assertEquals

open class Sep12ConfigTest {
    @Test
    fun testSep12ConfigValid() {
        val sep12Config = PropertySep12Config()
        sep12Config.customerIntegrationEndPoint = "http://localhost:8081"

        val errors = BindException(sep12Config, "sep12Config")
        ValidationUtils.invokeValidator(sep12Config, sep12Config, errors)
        assertEquals(0, errors.errorCount)
    }

    @Test
    fun testSep12ConfigBadFile() {
        val sep12Config = PropertySep12Config()

        val errors = BindException(sep12Config, "sep12Config")
        ValidationUtils.invokeValidator(sep12Config, sep12Config, errors)
        assertEquals(1, errors.errorCount)
        errors.message?.let { assertContains(it, "empty-customerIntegrationEndPoint") }
    }
}