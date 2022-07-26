package org.stellar.anchor.platform.config

import org.stellar.anchor.config.Sep31Config.DepositInfoGeneratorType.CIRCLE

import org.junit.jupiter.api.*
import org.springframework.validation.BindException
import org.springframework.validation.ValidationUtils
import kotlin.test.assertContains
import kotlin.test.assertEquals

open class Sep38ConfigTest {
    @Test
    fun testSep38ConfigValid() {
        val sep38Config = PropertySep38Config()
        sep38Config.quoteIntegrationEndPoint = "http://localhost:8081"

        val errors = BindException(sep38Config, "sep38Config")
        ValidationUtils.invokeValidator(sep38Config, sep38Config, errors)
        assertEquals(0, errors.errorCount)
    }
    @Test
    fun testSep38ConfigBadQuoteIntegrationEndpoint() {
        val sep38Config = PropertySep38Config()
        sep38Config.quoteIntegrationEndPoint = "not-a-url"

        val errors = BindException(sep38Config, "sep38Config")
        ValidationUtils.invokeValidator(sep38Config, sep38Config, errors)
        assertEquals(1, errors.errorCount)
        errors.message?.let { assertContains(it, "invalidUrl-quoteIntegrationEndpoint") }
    }
}