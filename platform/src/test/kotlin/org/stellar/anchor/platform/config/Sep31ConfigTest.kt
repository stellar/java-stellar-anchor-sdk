package org.stellar.anchor.platform.config

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.stellar.anchor.config.CustodyConfig
import org.stellar.anchor.config.Sep31Config.DepositInfoGeneratorType

class Sep31ConfigTest {
  lateinit var config: PropertySep31Config
  lateinit var errors: Errors
  lateinit var custodyConfig: CustodyConfig

  @BeforeEach
  fun setUp() {
    custodyConfig = mockk()
    every { custodyConfig.isCustodyIntegrationEnabled } returns false

    config = PropertySep31Config(custodyConfig)
    config.enabled = true
    errors = BindException(config, "config")
    config.depositInfoGeneratorType = DepositInfoGeneratorType.SELF
  }

  @Test
  fun `test valid sep24 configuration`() {
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @Test
  fun `test invalid deposit info generator type`() {
    config.depositInfoGeneratorType = DepositInfoGeneratorType.CUSTODY
    config.validate(config, errors)
    assertEquals("sep31-deposit-info-generator-type", errors.allErrors[0].code)
  }

  @Test
  fun `test valid sep24 configuration with custody integration`() {
    every { custodyConfig.isCustodyIntegrationEnabled } returns true
    config.depositInfoGeneratorType = DepositInfoGeneratorType.CUSTODY
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @Test
  fun `test invalid deposit info generator type with custody integration`() {
    every { custodyConfig.isCustodyIntegrationEnabled } returns true
    config.validate(config, errors)
    assertEquals("sep31-deposit-info-generator-type", errors.allErrors[0].code)
  }
}
