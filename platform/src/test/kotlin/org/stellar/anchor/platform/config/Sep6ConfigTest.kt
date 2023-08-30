package org.stellar.anchor.platform.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.stellar.anchor.config.Sep6Config

class Sep6ConfigTest {
  lateinit var config: PropertySep6Config
  lateinit var errors: Errors

  @BeforeEach
  fun setUp() {
    config = PropertySep6Config()
    config.enabled = true
    config.features = Sep6Config.Features(false, false)
    errors = BindException(config, "config")
  }

  @Test
  fun `test disabled sep6 configuration skips remaining validation`() {
    config.enabled = false

    config.features = null
    config.validate(config, errors)
    Assertions.assertFalse(errors.hasErrors())

    config.features = Sep6Config.Features(true, true)
    config.validate(config, errors)
    Assertions.assertFalse(errors.hasErrors())
  }

  @Test
  fun `test valid sep6 configuration`() {
    config.validate(config, errors)
    Assertions.assertFalse(errors.hasErrors())
  }

  @Test
  fun `test validation rejecting undefined features config`() {
    config.features = null
    config.validate(config, errors)
    Assertions.assertEquals("sep6-features-invalid", errors.allErrors[0].code)
  }

  @Test
  fun `test validation rejecting account creation enabled`() {
    config.features = Sep6Config.Features(true, false)
    config.validate(config, errors)
    Assertions.assertEquals("sep6-features-account-creation-invalid", errors.allErrors[0].code)
  }

  @Test
  fun `test validation rejecting claimable balances enabled`() {
    config.features = Sep6Config.Features(false, true)
    config.validate(config, errors)
    Assertions.assertEquals("sep6-features-claimable-balances-invalid", errors.allErrors[0].code)
  }
}
