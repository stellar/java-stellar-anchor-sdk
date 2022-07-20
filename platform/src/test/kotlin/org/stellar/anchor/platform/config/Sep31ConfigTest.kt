package org.stellar.anchor.platform.config

import org.stellar.anchor.config.Sep31Config.DepositInfoGeneratorType.CIRCLE

import org.junit.jupiter.api.*
import org.springframework.validation.BindException
import org.springframework.validation.ValidationUtils
import kotlin.test.assertContains
import kotlin.test.assertEquals

open class Sep31ConfigTest {
    @Test
    fun testSep31WithCircleConfig() {
        val circleConfig = PropertyCircleConfig()
        circleConfig.setCircleUrl("https://api-sandbox.circle.com")
        val sep31Config = PropertySep31Config(circleConfig)
        sep31Config.setDepositInfoGeneratorType(CIRCLE)

        val errors = BindException(sep31Config, "sep31Config")
        ValidationUtils.invokeValidator(sep31Config, sep31Config, errors)
        assertEquals(1, errors.errorCount)
        errors.message?.let { assertContains(it, "badConfig-circle") }

        //errors = BindException(circleConfig, "circleConfig")
        //ValidationUtils.invokeValidator(circleConfig, circleConfig, errors)
        val circleErrors = circleConfig.validate()
        assertEquals(1, circleErrors.errorCount)
        circleErrors.message?.let { assertContains(it, "empty-apiKey") }
    }
}