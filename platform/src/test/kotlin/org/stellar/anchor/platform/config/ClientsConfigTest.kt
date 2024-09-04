package org.stellar.anchor.platform.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.stellar.anchor.client.ClientConfig.CallbackUrls
import org.stellar.anchor.client.ClientConfig.ClientType.CUSTODIAL
import org.stellar.anchor.client.ClientConfig.ClientType.NONCUSTODIAL
import org.stellar.anchor.client.TempClient
import org.stellar.anchor.config.ClientsConfig

class ClientsConfigTest {
  private lateinit var config: PropertyClientsConfig
  private lateinit var errors: Errors

  @BeforeEach
  fun setup() {
    config = PropertyClientsConfig().apply { type = ClientsConfig.ClientsConfigType.INLINE }
    errors = BindException(config, "config")
  }

  @Test
  fun `test valid custodial client with multiple signing keys`() {
    val custodial =
      TempClient.builder()
        .name("custodial")
        .type(CUSTODIAL)
        .signingKeys(
          setOf(
            "GBI2IWJGR4UQPBIKPP6WG76X5PHSD2QTEBGIP6AZ3ZXWV46ZUSGNEGN2",
            "GACYKME36AI6UYAV7A5ZUA6MG4C4K2VAPNYMW5YLOM6E7GS6FSHDPV4F",
          )
        )
        .build()

    config.setItems(listOf(custodial))
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @Test
  fun `test invalid custodial client with empty signing key`() {
    val custodial =
      TempClient.builder().name("custodial").type(CUSTODIAL).signingKeys(setOf()).build()

    config.setItems(listOf(custodial))
    config.validate(config, errors)
    assertErrorCode(errors, "invalid-custodial-client-config")
  }

  @Test
  fun `test valid non-custodial client with multiple domains`() {
    val nonCustodial =
      TempClient.builder()
        .name("non-custodial")
        .type(NONCUSTODIAL)
        .domains(setOf("example.com", "example.org"))
        .build()

    config.setItems(listOf(nonCustodial))
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @Test
  fun `test valid non-custodial client with all callback URLs set`() {
    val nonCustodial =
      TempClient.builder()
        .name("non-custodial")
        .type(NONCUSTODIAL)
        .domains(setOf("example.com"))
        .callbackUrls(
          CallbackUrls.builder()
            .sep6("https://example.com/api/v1/anchor/callback/sep6")
            .sep24("https://example.com/api/v1/anchor/callback/sep24")
            .sep31("https://example.com/api/v1/anchor/callback/sep31")
            .sep12("https://example.com/api/v1/anchor/callback/sep12")
            .build()
        )
        .build()

    config.setItems(listOf(nonCustodial))
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @Test
  fun `test valid custodial client with all callback URLs set`() {
    val custodial =
      TempClient.builder()
        .name("custodial")
        .type(CUSTODIAL)
        .signingKeys(
          setOf(
            "GBI2IWJGR4UQPBIKPP6WG76X5PHSD2QTEBGIP6AZ3ZXWV46ZUSGNEGN2",
            "GACYKME36AI6UYAV7A5ZUA6MG4C4K2VAPNYMW5YLOM6E7GS6FSHDPV4F",
          )
        )
        .callbackUrls(
          CallbackUrls.builder()
            .sep6("https://example.com/api/v1/anchor/callback/sep6")
            .sep24("https://example.com/api/v1/anchor/callback/sep24")
            .sep31("https://example.com/api/v1/anchor/callback/sep31")
            .sep12("https://example.com/api/v1/anchor/callback/sep12")
            .build()
        )
        .build()

    config.setItems(listOf(custodial))
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @Test
  fun `test invalid non-custodial client with invalid callback URLs`() {
    val nonCustodial =
      TempClient.builder()
        .name("non-custodial")
        .type(NONCUSTODIAL)
        .domains(setOf("example.com"))
        .callbackUrls(
          CallbackUrls.builder()
            .sep6("bad-url")
            .sep24("bad-url")
            .sep31("bad-url")
            .sep12("bad-url")
            .build()
        )
        .build()

    config.setItems(listOf(nonCustodial))
    config.validate(config, errors)
    assertEquals(4, errors.errorCount)
  }

  @Test
  fun `test invalid custodial client with invalid callback URLs`() {
    val custodial =
      TempClient.builder()
        .name("custodial")
        .type(CUSTODIAL)
        .signingKeys(
          setOf(
            "GBI2IWJGR4UQPBIKPP6WG76X5PHSD2QTEBGIP6AZ3ZXWV46ZUSGNEGN2",
            "GACYKME36AI6UYAV7A5ZUA6MG4C4K2VAPNYMW5YLOM6E7GS6FSHDPV4F",
          )
        )
        .callbackUrls(
          CallbackUrls.builder()
            .sep6("bad-url")
            .sep24("bad-url")
            .sep31("bad-url")
            .sep12("bad-url")
            .build()
        )
        .build()

    config.setItems(listOf(custodial))
    config.validate(config, errors)
    assertEquals(4, errors.errorCount)
  }
}
