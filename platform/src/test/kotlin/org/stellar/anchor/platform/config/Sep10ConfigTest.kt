package org.stellar.anchor.platform.config

import io.mockk.every
import io.mockk.mockk
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
        "unknown",
        CUSTODIAL,
        "GBI2IWJGR4UQPBIKPP6WG76X5PHSD2QTEBGIP6AZ3ZXWV46ZUSGNEGN2",
        null,
        null
      )
    )

    clientsConfig.clients.add(
      ClientsConfig.ClientConfig(
        "lobstr",
        NONCUSTODIAL,
        "GC4HAYCFQYQLJV5SE6FB3LGC37D6XGIXGMAXCXWNBLH7NWW2JH4OZLHQ",
        "lobstr.co",
        "https://callback.lobstr.co/api/v2/anchor/callback"
      )
    )

    clientsConfig.clients.add(
      ClientsConfig.ClientConfig(
        "circle",
        NONCUSTODIAL,
        "GCSGSR6KQQ5BP2FXVPWRL6SWPUSFWLVONLIBJZUKTVQB5FYJFVL6XOXE",
        "circle.com",
        "https://callback.circle.com/api/v2/anchor/callback"
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
  }

  @Test
  fun `test validation of empty client allow list when client attribution is required`() {
    val config = PropertySep10Config(appConfig, ClientsConfig(), secretConfig)
    config.isClientAttributionRequired = true
    config.validateClientAttribution(errors)
    assertErrorCode(errors, "sep10-client-attribution-lists-empty")
  }

  @Test
  fun `test ClientsConfig getClientConfigByName`() {
    assertEquals(clientsConfig.getClientConfigByName("unknown"), clientsConfig.clients[0])
    assertEquals(clientsConfig.getClientConfigByName("lobstr"), clientsConfig.clients[1])
    assertEquals(clientsConfig.getClientConfigByName("circle"), clientsConfig.clients[2])
  }

  @Test
  fun `test ClientsConfig getClientConfigByDomain`() {
    assertEquals(clientsConfig.getClientConfigByDomain("unknown"), null)
    assertEquals(clientsConfig.getClientConfigByDomain("lobstr.co"), clientsConfig.clients[1])
    assertEquals(clientsConfig.getClientConfigByDomain("circle.com"), clientsConfig.clients[2])
  }

  @Test
  fun `test ClientsConfig getClientConfigBySigningKey`() {
    assertEquals(clientsConfig.getClientConfigBySigningKey("unknown"), null)
    assertEquals(
      clientsConfig.getClientConfigBySigningKey(
        "GC4HAYCFQYQLJV5SE6FB3LGC37D6XGIXGMAXCXWNBLH7NWW2JH4OZLHQ"
      ),
      clientsConfig.clients[1]
    )
    assertEquals(
      clientsConfig.getClientConfigBySigningKey(
        "GCSGSR6KQQ5BP2FXVPWRL6SWPUSFWLVONLIBJZUKTVQB5FYJFVL6XOXE"
      ),
      clientsConfig.clients[2]
    )
  }

  @Test
  fun `test when clientAllowList is not defined, clientAttributionAllowList equals to the list of all clients`() {
    val config = PropertySep10Config(appConfig, clientsConfig, secretConfig)
    assertEquals(config.clientAttributionAllowList, listOf("lobstr.co", "circle.com"))
  }

  @Test
  fun `test when clientAllowList is defined, clientAttributionAllowList returns correct values`() {
    val config = PropertySep10Config(appConfig, clientsConfig, secretConfig)
    config.clientAllowList = listOf("lobstr")
    assertEquals(config.clientAttributionAllowList, listOf("lobstr.co"))

    config.clientAllowList = listOf("circle")
    assertEquals(config.clientAttributionAllowList, listOf("circle.com"))

    config.clientAllowList = listOf("invalid")
    config.validateClientAttribution(errors)
    assertErrorCode(errors, "sep10-client-allow-list-invalid")
    assertTrue(config.clientAttributionAllowList.isEmpty())
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
        "stellar.org:1000:1000,sep10-web-auth-domain-invalid",
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
}
