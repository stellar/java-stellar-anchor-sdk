package org.stellar.anchor.util

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.TestConstants.Companion.TEST_ACCOUNT
import org.stellar.anchor.TestConstants.Companion.TEST_MEMO
import org.stellar.anchor.TestHelper
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.client.ClientFinder
import org.stellar.anchor.client.ClientService
import org.stellar.anchor.client.CustodialClientConfig
import org.stellar.anchor.client.NonCustodialClientConfig
import org.stellar.anchor.config.Sep10Config
import org.stellar.anchor.sep6.ExchangeAmountsCalculatorTest

class ClientFinderTest {
  companion object {
    val token = TestHelper.createSep10Jwt(TEST_ACCOUNT, TEST_MEMO)
    private val custodialClient =
      CustodialClientConfig.builder()
        .name("referenceCustodial")
        .signingKeys(setOf("signing-key"))
        .allowAnyDestination(false)
        .build()

    private val nonCustodialClient =
      NonCustodialClientConfig.builder()
        .name("reference")
        .domains(setOf("wallet-server:8092"))
        .callbackUrl("http://wallet-server:8092/callbacks")
        .build()
  }

  @MockK(relaxed = true) lateinit var sep10Config: Sep10Config
  @MockK(relaxed = true) lateinit var clientService: ClientService

  private lateinit var clientFinder: ClientFinder

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { sep10Config.isClientAttributionRequired } returns true
    every { sep10Config.allowedClientDomains } returns nonCustodialClient.domains.toList()
    every { sep10Config.allowedClientNames } returns
      listOf(custodialClient.name, nonCustodialClient.name)
    every {
      clientService.getClientConfigByDomain(ExchangeAmountsCalculatorTest.token.clientDomain)
    } returns nonCustodialClient
    every {
      clientService.getClientConfigBySigningKey(ExchangeAmountsCalculatorTest.token.account)
    } returns custodialClient

    clientFinder = ClientFinder(sep10Config, clientService)
  }

  @Test
  fun `test getClientName with client found by domain`() {
    every { clientService.getClientConfigByDomain(token.clientDomain) } returns nonCustodialClient
    val clientId = clientFinder.getClientName(token)

    assertEquals(nonCustodialClient.name, clientId)
  }

  @Test
  fun `test getClientName with client found by signing key`() {
    every { clientService.getClientConfigByDomain(token.clientDomain) } returns null
    val clientId = clientFinder.getClientName(token)

    assertEquals(custodialClient.name, clientId)
  }

  @Test
  fun `test getClientName with client not found`() {
    every { clientService.getClientConfigByDomain(token.clientDomain) } returns null
    every { clientService.getClientConfigBySigningKey(token.account) } returns null

    assertThrows<SepNotAuthorizedException> { clientFinder.getClientName(token) }
  }

  @Test
  fun `test getClientName with client not found by domain`() {
    every { sep10Config.allowedClientNames } returns listOf("nothing")

    assertThrows<SepNotAuthorizedException> { clientFinder.getClientName(token) }
  }

  @Test
  fun `test getClientName with client not found by name`() {
    every { sep10Config.allowedClientNames } returns listOf("nothing")

    assertThrows<SepNotAuthorizedException> { clientFinder.getClientName(token) }
  }

  @Test
  fun `test getClientName with all names allowed`() {
    every { sep10Config.allowedClientNames } returns
      listOf(custodialClient.name, nonCustodialClient.name)
    val clientId = clientFinder.getClientName(token)

    assertEquals(nonCustodialClient.name, clientId)
  }

  @Test
  fun `test getClientName with client attribution disabled and missing client`() {
    every { sep10Config.isClientAttributionRequired } returns false
    every { clientService.getClientConfigByDomain(token.clientDomain) } returns null
    every { clientService.getClientConfigBySigningKey(token.account) } returns null

    val clientId = clientFinder.getClientName(token)
    assertNull(clientId)
  }

  @Test
  fun `test getClientName with client attribution disabled and client found by signing key`() {
    every { sep10Config.isClientAttributionRequired } returns false
    every { clientService.getClientConfigByDomain(token.clientDomain) } returns null
    every { clientService.getClientConfigBySigningKey(token.account) } returns custodialClient

    val clientId = clientFinder.getClientName(token)
    assertEquals(custodialClient.name, clientId)
  }

  @Test
  fun `test getClientName with client attribution disabled and client found by domain`() {
    every { sep10Config.isClientAttributionRequired } returns false
    every { clientService.getClientConfigByDomain(token.clientDomain) } returns nonCustodialClient
    every { clientService.getClientConfigBySigningKey(token.account) } returns null

    val clientId = clientFinder.getClientName(token)
    assertEquals(nonCustodialClient.name, clientId)
  }
}
