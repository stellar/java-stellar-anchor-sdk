package org.stellar.anchor.platform.config

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.stellar.anchor.auth.AuthConfig
import org.stellar.anchor.auth.AuthType.JWT

class CallbackApiConfigTest {
  lateinit var config: CallbackApiConfig
  lateinit var errors: Errors
  lateinit var secretConfig: PropertySecretConfig

  @BeforeEach
  fun setUp() {
    secretConfig = mockk()
    config = CallbackApiConfig(secretConfig)
    errors = BindException(config, "config")
  }

  @Test
  fun `test base_url`() {
    config.baseUrl = "http://localhost:8080"
    config.validateBaseUrl(config, errors)
    assertEquals(0, errors.errorCount)

    config.baseUrl = "https://www.stellar.org"
    config.validateBaseUrl(config, errors)
    assertEquals(0, errors.errorCount)
  }

  @Test
  fun `test mal-formatted url`() {
    // mal-formatted base_url
    config.baseUrl = "http://localhost; 8080"
    config.validateBaseUrl(config, errors)
    assertEquals(1, errors.errorCount)
    assertEquals("mal-formatted-callback-api-base-url", errors.allErrors[0].code)
  }

  @Test
  fun `test empty url`() {
    // empty base_url
    config.baseUrl = ""
    config.validateBaseUrl(config, errors)
    assertEquals(2, errors.errorCount)
    assertEquals("empty-callback-api-base-url", errors.allErrors[0].code)
  }

  @Test
  fun `test JWT_TOKEN callback api secret`() {
    every { secretConfig.callbackApiSecret } returns "secret"
    config.setAuth(
      AuthConfig(
        JWT,
        null,
        AuthConfig.JwtConfig("30000", "Authorization"),
        AuthConfig.ApiKeyConfig("X-Api-Key")
      )
    )
    config.validateAuth(config, errors)
    assertEquals(0, errors.errorCount)
  }

  @ParameterizedTest
  @ValueSource(strings = [""])
  @NullSource
  fun `test empty secret`(secretValue: String?) {
    every { secretConfig.callbackApiSecret } returns secretValue
    config.setAuth(
      AuthConfig(
        JWT,
        null,
        AuthConfig.JwtConfig("30000", "Authorization"),
        AuthConfig.ApiKeyConfig("X-Api-Key")
      )
    )
    config.validateAuth(config, errors)
    assertEquals(1, errors.errorCount)
    assertEquals("empty-secret-callback-api-secret", errors.allErrors[0].code)
  }
}
