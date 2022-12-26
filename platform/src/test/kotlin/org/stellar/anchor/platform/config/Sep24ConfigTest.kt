package org.stellar.anchor.platform.config

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.stellar.anchor.platform.config.PropertySep24Config.InteractiveUrlConfig
import org.stellar.anchor.platform.config.PropertySep24Config.SimpleMoreInfoUrlConfig

class Sep24ConfigTest {
  lateinit var config: PropertySep24Config
  lateinit var errors: Errors

  @BeforeEach
  fun setUp() {
    config = PropertySep24Config()
    config.enabled = true
    errors = BindException(config, "config")
    config.interactiveJwtExpiration = 1200
    config.interactiveUrl =
      InteractiveUrlConfig(
        "simple",
        PropertySep24Config.SimpleInteractiveUrlConfig("https://www.stellar.org", listOf(""))
      )
  }

  @Test
  fun `test valid sep24 configuration`() {
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @ParameterizedTest
  @ValueSource(ints = [-1, Integer.MIN_VALUE, 0])
  fun `test bad interactive jwt expiration`(expiration: Int) {
    config.interactiveJwtExpiration = expiration

    config.validate(config, errors)
    assertTrue(errors.hasErrors())
    assertErrorCode(errors, "sep24-interactive-jwt-expiration-invalid")
  }

  @ParameterizedTest
  @ValueSource(strings = ["httpss://www.stellar.org"])
  fun `test bad url`(url: String) {
    config.interactiveUrl =
      InteractiveUrlConfig(
        "simple",
        PropertySep24Config.SimpleInteractiveUrlConfig(url, listOf(""))
      )

    config.moreInfoUrl =
      PropertySep24Config.MoreInfoUrlConfig(
        "simple",
        SimpleMoreInfoUrlConfig(url, listOf(""), 1200)
      )

    config.validate(config, errors)
    assertTrue(errors.hasErrors())
  }
}
