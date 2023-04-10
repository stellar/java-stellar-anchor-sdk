package org.stellar.anchor.platform.config

import io.mockk.every
import io.mockk.mockk
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.validation.BindException
import org.springframework.validation.Errors

class Sep10ConfigTest {
  lateinit var config: PropertySep10Config
  lateinit var errors: Errors
  lateinit var secretConfig: PropertySecretConfig

  @BeforeEach
  fun setUp() {
    secretConfig = mockk()
    config = PropertySep10Config(secretConfig)
    config.enabled = true
    errors = BindException(config, "config")
    every { secretConfig.sep10SigningSeed } returns
      "SDNMFWJGLVR4O2XV3SNEJVF53MMLQWYFYFC7HT7JZ5235AXPETHB4K3D"
    every { secretConfig.sep10JwtSecretKey } returns "secret"
  }

  @Test
  fun `test default sep10 config`() {
    config.validateConfig(errors)
    assertFalse(errors.hasErrors())
  }

  @Test
  fun `test client attribution and lists`() {
    config.isClientAttributionRequired = true
    config.clientAttributionAllowList = listOf("stellar.org", "lobstr.com")
    config.validateClientAttribution(errors)
    assertFalse(errors.hasErrors())

    config.clientAttributionAllowList = listOf()
    config.clientAttributionDenyList = listOf("stellar.org", "lobstr.com")
    config.validateClientAttribution(errors)
    assertFalse(errors.hasErrors())
  }

  @Test
  fun `test client attribution allow list is not empty while client_attrbution_required is set to false`() {
    config.isClientAttributionRequired = false
    config.clientAttributionAllowList = listOf("stellar.org", "lobstr.com")
    config.validateClientAttribution(errors)
    assertErrorCode(errors, "sep10-client-attribution-allow-list-not-empty")
  }

  @Test
  fun `test client attribution deny list is not empty while client_attrbution_required is set to false`() {
    config.isClientAttributionRequired = false
    config.clientAttributionDenyList = listOf("stellar.org", "lobstr.com")
    config.validateClientAttribution(errors)
    assertErrorCode(errors, "sep10-client-attribution-deny-list-not-empty")
  }

  @Test
  fun `test both client attribution deny and allow lists are defined`() {
    config.isClientAttributionRequired = true
    config.clientAttributionAllowList = listOf("stellar.org", "lobstr.com")
    config.clientAttributionDenyList = listOf("mgi.com")
    config.validateClientAttribution(errors)
    assertErrorCode(errors, "sep10-client-attribution-lists-conflict")
  }

  @Test
  fun `test both client attribution deny and allow lists are empty`() {
    config.isClientAttributionRequired = true
    config.clientAttributionAllowList = listOf()
    config.clientAttributionDenyList = listOf()
    config.validateClientAttribution(errors)
    assertErrorCode(errors, "sep10-client-attribution-lists-empty")
  }

  @ParameterizedTest
  @MethodSource("validOmnibusAccounts")
  fun `test omnibus account list`(omnibusAccounts: List<String>) {
    config.omnibusAccountList = omnibusAccounts
    config.validateOmnibusAccounts(errors)
    assertFalse(errors.hasErrors())
  }

  @ParameterizedTest
  @MethodSource("invalidOmnibusAccounts")
  fun `test invalid omnibus account list`(omnibusAccounts: List<String>) {
    config.omnibusAccountList = omnibusAccounts
    config.validateOmnibusAccounts(errors)
    assertErrorCode(errors, "sep10-omnibus-account-not-valid")
  }

  @Test
  fun `test required known omnibus account`() {
    config.isRequireKnownOmnibusAccount = true
    config.omnibusAccountList = listOf("GCS2KBEGIWILNKFYY6ORT72Y2HUFYG6IIIOESHVQC3E5NIYT3L2I5F5E")
    config.validateOmnibusAccounts(errors)
    assertFalse(errors.hasErrors())
  }

  @Test
  fun `test known omnibus account required but not defined`() {
    config.isRequireKnownOmnibusAccount = true
    config.omnibusAccountList = listOf()
    config.validateOmnibusAccounts(errors)
    assertErrorCode(errors, "sep10-omnibus-account-list-empty")
  }

  @ParameterizedTest
  @ValueSource(strings = ["stellar.org", "moneygram.com"])
  fun `test valid home domains`(value: String) {
    config.webAuthDomain = value
    config.validateConfig(errors)
    assertFalse(errors.hasErrors())
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["bad key", "GCS2KBEGIWILNKFYY6ORT72Y2HUFYG6IIIOESHVQC3E5NIYT3L2I5F5E"])
  fun `test invalid sep10 seeds`(value: String?) {
    every { secretConfig.sep10SigningSeed } returns value
    config.validateConfig(errors)
    assertTrue(errors.hasErrors())
  }

  @ParameterizedTest
  @ValueSource(strings = ["stellar .org", "abc", "299.0.0.1"])
  fun `test invalid web auth domains`(value: String) {
    config.webAuthDomain = value
    config.validateConfig(errors)
    assertTrue(errors.hasErrors())
    assertErrorCode(errors, "sep10-web-auth-domain-invalid")
  }

  companion object {
    @JvmStatic
    fun validOmnibusAccounts(): Stream<List<String>> {
      return Stream.of(
        listOf(),
        listOf("GAU2XSVTXY6GADVFFLDJLWO44SC6MAWPMHZTI4QHYUKV6BGGJFAIEYGB"),
        listOf(
          "GCS2KBEGIWILNKFYY6ORT72Y2HUFYG6IIIOESHVQC3E5NIYT3L2I5F5E",
          "GAU2XSVTXY6GADVFFLDJLWO44SC6MAWPMHZTI4QHYUKV6BGGJFAIEYGB"
        )
      )
    }
    @JvmStatic
    fun invalidOmnibusAccounts(): Stream<List<String>> {
      return Stream.of(
        listOf("SBBGHY3KIEI4XM2G2MD76DB3F3EPC6A2NR57CY2PFJVE66T7UTAE3SKD"),
        listOf(
          "GCS2KBEGIWILNKFYY6ORT72Y2HUFYG6IIIOESHVQC3E5NIYT3L2I5F5E",
          "SBBGHY3KIEI4XM2G2MD76DB3F3EPC6A2NR57CY2PFJVE66T7UTAE3SKD"
        ),
        listOf("1234")
      )
    }
  }
}
