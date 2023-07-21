package org.stellar.anchor.platform.config

import io.mockk.every
import io.mockk.mockk
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.platform.config.ClientsConfig.ClientType.CUSTODIAL
import org.stellar.anchor.platform.config.ClientsConfig.ClientType.NONCUSTODIAL

class Sep10ConfigTest {
  lateinit var config: PropertySep10Config
  lateinit var errors: Errors
  private lateinit var secretConfig: PropertySecretConfig
  private lateinit var appConfig: AppConfig
  private var clientsConfig = ClientsConfig()

  @BeforeEach
  fun setUp() {
    secretConfig = mockk()
    appConfig = mockk()

    clientsConfig.clients.add(
      ClientsConfig.ClientConfig(
        CUSTODIAL,
        "GBI2IWJGR4UQPBIKPP6WG76X5PHSD2QTEBGIP6AZ3ZXWV46ZUSGNEGN2",
        null,
        null
      )
    )

    clientsConfig.clients.add(
      ClientsConfig.ClientConfig(
        NONCUSTODIAL,
        "GC4HAYCFQYQLJV5SE6FB3LGC37D6XGIXGMAXCXWNBLH7NWW2JH4OZLHQ",
        "lobstr.co",
        "https://callback.lobstr.co/api/v2/anchor/callback"
      )
    )

    clientsConfig.clients.add(
      ClientsConfig.ClientConfig(
        NONCUSTODIAL,
        "GCSGSR6KQQ5BP2FXVPWRL6SWPUSFWLVONLIBJZUKTVQB5FYJFVL6XOXE",
        "stellar.org",
        "https://callback.stellar.org/api/v2/anchor/callback"
      )
    )

    config = PropertySep10Config(appConfig, clientsConfig, secretConfig)
    config.enabled = true
    config.homeDomain = "stellar.org"
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
    config.validateClientAttribution(errors)
    assertFalse(errors.hasErrors())
    println(config.clientAttributionAllowList)
    println(config.knownCustodialAccountList)
  }

  @Test
  fun `test both client attribution deny and allow lists are empty`() {
    // Create a cofnig with empty ClientsConfig lists
    val config = PropertySep10Config(appConfig, ClientsConfig(), secretConfig)
    config.isClientAttributionRequired = true
    config.validateClientAttribution(errors)
    assertErrorCode(errors, "sep10-client-attribution-lists-empty")
  }

  @Test
  fun `test required known custodial account`() {
    config.isKnownCustodialAccountRequired = true
    config.validateCustodialAccounts(errors)
    assertFalse(errors.hasErrors())
  }

  @Test
  fun `test known custodial account required but no custodial clients not defined`() {
    config = PropertySep10Config(appConfig, ClientsConfig(), secretConfig)
    config.isKnownCustodialAccountRequired = true
    config.validateCustodialAccounts(errors)
    assertErrorCode(errors, "sep10-custodial-account-list-empty")
  }

  @ParameterizedTest
  @ValueSource(strings = ["stellar.org", "moneygram.com", "localhost", "127.0.0.1:80"])
  fun `test valid home domains`(value: String) {
    config.webAuthDomain = value
    config.homeDomain = value
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
  @CsvSource(
    value =
      [
        "this-is-longer-than-64-bytes-which-is-the-maximum-length-for-a-web-auth-domain.stellar.org,sep10-web-auth-domain-too-long",
        "stellar .org,sep10-web-auth-domain-invalid",
        "abc,sep10-web-auth-domain-invalid",
        "299.0.0.1,sep10-web-auth-domain-invalid",
      ]
  )
  fun `test invalid web auth domains`(value: String, expectedErrorCode: String) {
    config.webAuthDomain = value
    config.validateConfig(errors)
    assertTrue(errors.hasErrors())
    assertErrorCode(errors, expectedErrorCode)
  }

  @ParameterizedTest
  @CsvSource(
    value =
      [
        "this-is-longer-than-64-bytes-which-is-the-maximum-length-for-a-home-domain.stellar.org,sep10-home-domain-too-long",
        "stellar .org,sep10-home-domain-invalid",
        "abc,sep10-home-domain-invalid",
        "299.0.0.1,sep10-home-domain-invalid",
        "http://stellar.org,sep10-home-domain-invalid",
        "https://stellar.org,sep10-home-domain-invalid",
        "://stellar.org,sep10-home-domain-invalid",
      ]
  )
  fun `test invalid home domains`(value: String, expectedErrorCode: String) {
    config.homeDomain = value
    config.validateConfig(errors)
    assertTrue(errors.hasErrors())
    assertErrorCode(errors, expectedErrorCode)
  }

  @Test
  fun `test if web_auth_domain is not set, default to the domain of the host_url`() {
    config.webAuthDomain = null
    config.homeDomain = "www.stellar.org"
    config.postConstruct()
    assertEquals("www.stellar.org", config.webAuthDomain)
  }

  @Test
  fun `test if web_auth_domain is set, it is not default to the domain of the host_url`() {
    config.webAuthDomain = "localhost:8080"
    config.homeDomain = "www.stellar.org"
    config.postConstruct()
    assertEquals("localhost:8080", config.webAuthDomain)
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
