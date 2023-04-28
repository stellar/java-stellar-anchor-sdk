package org.stellar.anchor.platform.config

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.stellar.anchor.auth.AuthConfig
import org.stellar.anchor.auth.AuthType.JWT
import org.stellar.anchor.auth.AuthType.NONE
import org.stellar.anchor.config.SecretConfig

class CustodyApiConfigTest {

  private lateinit var config: CustodyApiConfig
  private lateinit var secretConfig: SecretConfig
  private lateinit var errors: Errors

  @BeforeEach
  fun setUp() {
    secretConfig = mockk()
    every { secretConfig.custodyApiSecret } returns "testCustodyApiSecret"
    config = CustodyApiConfig(secretConfig)
    config.baseUrl = "https://test.com"
    config.auth = AuthConfig()
    config.auth.type = JWT
    errors = BindException(config, "config")
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = [""])
  fun `test empty host_url`(url: String?) {
    config.baseUrl = url
    config.validate(config, errors)
    assertErrorCode(errors, "custody-server-base-url-empty")
  }

  @ParameterizedTest
  @ValueSource(strings = ["https://custody.com", "https://custody.org:8080"])
  fun `test valid host_url`(url: String) {
    config.baseUrl = url
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @ParameterizedTest
  @ValueSource(strings = ["https ://custody.com", "custody.com", "abc"])
  fun `test invalid host_url`(url: String) {
    config.baseUrl = url
    config.validate(config, errors)
    assertErrorCode(errors, "custody-server-base-url-invalid")
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = [""])
  fun `test empty api_key`(apiKey: String?) {
    every { secretConfig.custodyApiSecret } returns apiKey
    config.validate(config, errors)
    assertErrorCode(errors, "empty-secret-custody-server-secret")
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = [""])
  fun `test empty api_key with NONE auth type`(apiKey: String?) {
    every { secretConfig.custodyApiSecret } returns apiKey
    config.auth.type = NONE
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @Test
  fun `test valid api_key`() {
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }
}
