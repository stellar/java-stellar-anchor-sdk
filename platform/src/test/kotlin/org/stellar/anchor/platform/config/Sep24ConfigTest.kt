package org.stellar.anchor.platform.config

import java.lang.Integer.MIN_VALUE
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.stellar.anchor.platform.config.PropertySep24Config.*

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
    config.moreInfoUrl =
      MoreInfoUrlConfig(
        "simple",
        SimpleMoreInfoUrlConfig("https://www.stellar.org", listOf(""), 10)
      )
  }

  @Test
  fun `test valid sep24 configuration`() {
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @ParameterizedTest
  @ValueSource(ints = [-1, MIN_VALUE, 0])
  fun `test bad interactive jwt expiration`(expiration: Int) {
    config.interactiveJwtExpiration = expiration

    config.validate(config, errors)
    assertTrue(errors.hasErrors())
    assertErrorCode(errors, "sep24-interactive-jwt-expiration-invalid")
  }

  @ParameterizedTest
  @ValueSource(strings = ["httpss://www.stellar.org"])
  fun `test interactive url with bad url configuration`(url: String) {
    config.interactiveUrl =
      InteractiveUrlConfig(
        "simple",
        PropertySep24Config.SimpleInteractiveUrlConfig(url, listOf(""))
      )

    config.validate(config, errors)
    assertEquals("sep24-interactive-url-simple-base-url-not-valid", errors.allErrors[0].code)
  }

  @ParameterizedTest
  @ValueSource(strings = ["httpss://www.stellar.org"])
  fun `test more_info_url with invalid url`(url: String) {
    config.moreInfoUrl = MoreInfoUrlConfig("simple", SimpleMoreInfoUrlConfig(url, listOf(""), 100))

    config.validate(config, errors)
    assertEquals("sep24-more-info-url-simple-base-url-not-valid", errors.allErrors[0].code)
  }

  @ParameterizedTest
  @ValueSource(ints = [-1, MIN_VALUE, 0])
  fun `test more_info_url with invalid jwt_expiration`(expiration: Int) {
    config.moreInfoUrl =
      MoreInfoUrlConfig(
        "simple",
        SimpleMoreInfoUrlConfig("https://www.stellar.org", listOf(""), expiration)
      )

    config.validate(config, errors)
    assertEquals("sep24-more-info-url-simple-jwt-expiration-not-valid", errors.allErrors[0].code)
  }
}
