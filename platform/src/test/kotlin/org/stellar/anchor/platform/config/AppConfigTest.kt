package org.stellar.anchor.platform.config

import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.validation.BindException
import org.springframework.validation.Errors

class AppConfigTest {
  private lateinit var config: PropertyAppConfig
  private lateinit var errors: Errors

  @BeforeEach
  fun setUp() {
    config = PropertyAppConfig()
    config.stellarNetworkPassphrase = "Test SDF Network ; September 2015"
    config.horizonUrl = "https://horizon-testnet.stellar.org"
    errors = BindException(config, "config")
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = [""])
  fun `test empty horizon_url`(url: String?) {
    config.horizonUrl = url
    config.validateConfig(config, errors)
    assertErrorCode(errors, "horizon-url-empty")
  }
  @ParameterizedTest
  @ValueSource(
    strings = ["https://horizon-testnet.stellar.org", "https://horizon-testnet.stellar.org:8080"]
  )
  fun `test valid horizon_url`(url: String) {
    config.horizonUrl = url
    config.validateConfig(config, errors)
    assertFalse(errors.hasErrors())
  }

  @ParameterizedTest
  @ValueSource(strings = ["https://horizon-testnet.stellar. org", "stellar.org", "abc"])
  fun `test invalid horizon_url`(url: String) {
    config.horizonUrl = url
    config.validateConfig(config, errors)
    assertErrorCode(errors, "horizon-url-invalid")
  }

  @ParameterizedTest
  @NullSource
  @MethodSource("validLanguages")
  fun `test valid languages`(langs: List<String>?) {
    config.languages = langs
    config.validateLanguage(config, errors)
    assertFalse(errors.hasErrors())
  }

  @ParameterizedTest
  @MethodSource("invalidLanguages")
  fun `test invalid languages`(langs: List<String>) {
    config.languages = langs
    config.validateLanguage(config, errors)
    assertErrorCode(errors, "languages-invalid")
  }

  companion object {
    @JvmStatic
    fun validLanguages(): Stream<List<String>> {
      return Stream.of(listOf(), listOf("en", "en-us", "EN", "EN-US"), listOf("zh-tw", "zh"))
    }
    @JvmStatic
    fun invalidLanguages(): Stream<List<String>> {
      return Stream.of(listOf("1234", "EN", "EN-US"))
    }
  }
}
