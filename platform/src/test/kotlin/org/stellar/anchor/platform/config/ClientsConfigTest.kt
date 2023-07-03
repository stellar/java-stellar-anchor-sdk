package org.stellar.anchor.platform.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.validation.BindException
import org.springframework.validation.Errors

class ClientsConfigTest {
  private lateinit var config: ClientsConfig
  private lateinit var errors: Errors

  @BeforeEach
  fun setUp() {
    config = ClientsConfig()
    errors = BindException(config, "config")
  }
  @Test
  fun `test valid custodial client`() {
    config.type = ClientsConfig.ClientType.CUSTODIAL
    config.signingKey = "GBI2IWJGR4UQPBIKPP6WG76X5PHSD2QTEBGIP6AZ3ZXWV46ZUSGNEGN2"
    config.callbackUrl = "https://callback.circle.com/api/v1/anchor/callback"
    config.validate(config, errors)
    Assertions.assertFalse(errors.hasErrors())
  }

  @Test
  fun `test invalid custodial client with empty signing key`() {
    config.signingKey = ""
    config.validateCustodialClient(errors)
    Assertions.assertEquals(1, errors.errorCount)
    assertErrorCode(errors, "empty-client-signing-key")
  }

  @Test
  fun `test valid non-custodial client`() {
    config.type = ClientsConfig.ClientType.NONCUSTODIAL
    config.domain = "lobstr.co"
    config.callbackUrl = "https://callback.lobstr.co/api/v2/anchor/callback"
    config.signingKey = "GC4HAYCFQYQLJV5SE6FB3LGC37D6XGIXGMAXCXWNBLH7NWW2JH4OZLHQ"
    config.validate(config, errors)
    Assertions.assertFalse(errors.hasErrors())
  }

  @Test
  fun `test invalid non-custodial client with empty domain`() {
    config.domain = ""
    config.callbackUrl = "  "
    config.validateNonCustodialClient(errors)
    Assertions.assertEquals(1, errors.errorCount)
    assertErrorCode(errors, "empty-client-domain")
  }

  @Test
  fun `test invalid non-custodial client signing key does not match`() {
    config.domain = "lobstr.co"
    config.callbackUrl = "https://callback.lobstr.co/api/v2/anchor/callback"
    config.signingKey = "Invalid-signing-key"
    config.validateNonCustodialClient(errors)
    Assertions.assertEquals(1, errors.errorCount)
    assertErrorCode(errors, "client-signing-key-does-not-match")
  }
}
