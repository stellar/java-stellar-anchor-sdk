package org.stellar.anchor.platform.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.stellar.anchor.platform.config.ClientsConfig.ClientConfig

class ClientsConfigTest {
  private lateinit var configs: ClientsConfig
  private lateinit var errors: Errors

  @BeforeEach
  fun setUp() {
    configs = ClientsConfig()
    errors = BindException(configs, "config")
  }
  @Test
  fun `test valid custodial client`() {
    val config = ClientConfig()
    config.name = "circle"
    config.type = ClientsConfig.ClientType.CUSTODIAL
    config.signingKey = "GBI2IWJGR4UQPBIKPP6WG76X5PHSD2QTEBGIP6AZ3ZXWV46ZUSGNEGN2"
    config.callbackUrl = "https://callback.circle.com/api/v1/anchor/callback"
    configs.clients.add(config)

    configs.validate(configs, errors)
    Assertions.assertFalse(errors.hasErrors())
  }

  @Test
  fun `test invalid custodial client with empty signing key`() {
    val config = ClientConfig()
    config.signingKey = ""
    configs.clients.add(config)

    configs.validateCustodialClient(config, errors)
    Assertions.assertEquals(1, errors.errorCount)
    assertErrorCode(errors, "empty-client-signing-key")
  }

  @Test
  fun `test valid non-custodial client`() {
    val config = ClientConfig()
    config.name = "lobstr"
    config.type = ClientsConfig.ClientType.NONCUSTODIAL
    config.domain = "lobstr.co"
    config.callbackUrl = "https://callback.lobstr.co/api/v2/anchor/callback"
    config.signingKey = "GC4HAYCFQYQLJV5SE6FB3LGC37D6XGIXGMAXCXWNBLH7NWW2JH4OZLHQ"
    configs.clients.add(config)

    configs.validate(configs, errors)
    Assertions.assertFalse(errors.hasErrors())
  }

  @Test
  fun `test invalid non-custodial client with empty domain and callback url`() {
    val config = ClientConfig()
    config.domain = ""
    config.callbackUrl = "  "
    configs.clients.add(config)

    configs.validateNonCustodialClient(config, errors)
    Assertions.assertEquals(2, errors.errorCount)
  }

  @Test
  fun `test invalid non-custodial client signing key does not match`() {
    val config = ClientConfig()
    config.domain = "lobstr.co"
    config.callbackUrl = "https://callback.lobstr.co/api/v2/anchor/callback"
    config.signingKey = "Invalid-signing-key"
    configs.clients.add(config)

    configs.validateNonCustodialClient(config, errors)
    Assertions.assertEquals(1, errors.errorCount)
    assertErrorCode(errors, "client-signing-key-does-not-match")
  }
}
