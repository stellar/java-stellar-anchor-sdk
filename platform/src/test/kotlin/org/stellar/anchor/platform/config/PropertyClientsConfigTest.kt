package org.stellar.anchor.platform.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.stellar.anchor.config.ClientsConfig.ClientConfig
import org.stellar.anchor.config.ClientsConfig.ClientType.CUSTODIAL
import org.stellar.anchor.config.ClientsConfig.ClientType.NONCUSTODIAL

class PropertyClientsConfigTest {
  private lateinit var configs: PropertyClientsConfig
  private lateinit var errors: Errors

  @BeforeEach
  fun setUp() {
    configs = PropertyClientsConfig()
    errors = BindException(configs, "config")
  }

  @Test
  fun `test postConstruct`() {
    val config = ClientConfig()
    config.name = "sampleName"
    config.signingKey = "sampleSigningKey"
    config.domain = "sampleDomain"

    configs.clients.add(config)
    configs.postConstruct()

    Assertions.assertEquals(config.signingKey, config.signingKeys.first())
    Assertions.assertEquals(config.domain, config.domains.first())
  }

  @Test
  fun `test valid custodial client with multiple signing keys`() {
    val config = ClientConfig()
    config.name = "circle"
    config.type = CUSTODIAL
    config.signingKeys =
      setOf(
        "GBI2IWJGR4UQPBIKPP6WG76X5PHSD2QTEBGIP6AZ3ZXWV46ZUSGNEGN2",
        "GACYKME36AI6UYAV7A5ZUA6MG4C4K2VAPNYMW5YLOM6E7GS6FSHDPV4F"
      )
    config.callbackUrl = "https://callback.circle.com/api/v1/anchor/callback"
    configs.clients.add(config)

    configs.validate(configs, errors)
    Assertions.assertFalse(errors.hasErrors())
  }

  @Test
  fun `test invalid custodial client with empty signing key`() {
    val config = ClientConfig()
    config.signingKey = ""
    config.signingKeys = emptySet()
    configs.clients.add(config)

    configs.validateCustodialClient(config, errors)
    Assertions.assertEquals(1, errors.errorCount)
    assertErrorCode(errors, "empty-client-signing-keys")
  }

  @Test
  fun `test valid non-custodial client with multiple domains`() {
    val config = ClientConfig()
    config.name = "lobstr"
    config.type = NONCUSTODIAL
    config.domains = setOf("lobstr.co", "lobstr.com")
    config.callbackUrl = "https://callback.lobstr.co/api/v2/anchor/callback"
    configs.clients.add(config)

    configs.validate(configs, errors)
    Assertions.assertFalse(errors.hasErrors())
  }

  @Test
  fun `test invalid non-custodial client with empty domain and callback url`() {
    val config = ClientConfig()
    config.domain = ""
    config.domains = emptySet()
    config.callbackUrl = "  "
    configs.clients.add(config)

    configs.validateNonCustodialClient(config, errors)
    Assertions.assertEquals(2, errors.errorCount)
  }
}
