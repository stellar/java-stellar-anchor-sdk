package org.stellar.anchor.sep6

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.TestConstants
import org.stellar.anchor.TestConstants.Companion.TEST_ACCOUNT
import org.stellar.anchor.TestConstants.Companion.TEST_ASSET
import org.stellar.anchor.TestConstants.Companion.TEST_ASSET_SEP38_FORMAT
import org.stellar.anchor.TestConstants.Companion.TEST_CUSTOMER_ID
import org.stellar.anchor.TestHelper
import org.stellar.anchor.api.callback.FeeIntegration
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.api.callback.GetFeeResponse
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.SepValidationException
import org.stellar.anchor.api.sep.sep38.RateFee
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.config.ClientsConfig
import org.stellar.anchor.config.ClientsConfig.ClientConfig
import org.stellar.anchor.config.Sep10Config
import org.stellar.anchor.sep38.PojoSep38Quote
import org.stellar.anchor.sep38.Sep38QuoteStore
import org.stellar.anchor.sep6.ExchangeAmountsCalculator.Amounts

class ExchangeAmountsCalculatorTest {
  companion object {
    val token = TestHelper.createSep10Jwt(TEST_ACCOUNT, TestConstants.TEST_MEMO)
    val clientConfig =
      ClientConfig(
        "name",
        ClientsConfig.ClientType.CUSTODIAL,
        "signing-key",
        "domain",
        "http://localhost:8000",
        false,
        emptySet()
      )
  }

  private val assetService: AssetService = DefaultAssetService.fromJsonResource("test_assets.json")

  @MockK(relaxed = true) lateinit var sep10Config: Sep10Config
  @MockK(relaxed = true) lateinit var clientsConfig: ClientsConfig
  @MockK(relaxed = true) lateinit var feeIntegration: FeeIntegration
  @MockK(relaxed = true) lateinit var sep38QuoteStore: Sep38QuoteStore

  private lateinit var calculator: ExchangeAmountsCalculator

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { sep10Config.isClientAttributionRequired } returns true
    every { sep10Config.allowedClientDomains } returns listOf(clientConfig.domain)
    every { sep10Config.allowedClientNames } returns listOf(clientConfig.name)
    every { clientsConfig.getClientConfigByDomain(token.clientDomain) } returns clientConfig
    every { clientsConfig.getClientConfigBySigningKey(token.account) } returns clientConfig
    every {
      feeIntegration.getFee(
        GetFeeRequest.builder()
          .sendAmount("100")
          .sendAsset(TEST_ASSET_SEP38_FORMAT)
          .receiveAsset("iso4217:USD")
          .senderId(TEST_CUSTOMER_ID)
          .receiverId(TEST_CUSTOMER_ID)
          .clientId(clientConfig.name)
          .build()
      )
    } returns GetFeeResponse(Amount("2", "iso4217:USD"))
    calculator =
      ExchangeAmountsCalculator(
        sep10Config,
        clientsConfig,
        feeIntegration,
        sep38QuoteStore,
        assetService
      )
  }

  private val usdcQuote =
    PojoSep38Quote().apply {
      sellAsset = TEST_ASSET
      sellAmount = "100"
      buyAsset = "iso4217:USD"
      buyAmount = "98"
      fee =
        RateFee().apply {
          total = "2"
          asset = "iso4217:USD"
        }
    }

  @Test
  fun `test calculateFromQuote`() {
    val quoteId = "id"
    every { sep38QuoteStore.findByQuoteId(quoteId) } returns usdcQuote

    val result = calculator.calculateFromQuote(quoteId, assetService.getAsset("USDC"), "100")
    assertEquals(
      Amounts.builder()
        .amountIn("100")
        .amountInAsset("USDC")
        .amountOut("98")
        .amountOutAsset("iso4217:USD")
        .amountFee("2")
        .amountFeeAsset("iso4217:USD")
        .build(),
      result
    )
  }

  @Test
  fun `test calculateFromQuote with invalid quote id`() {
    every { sep38QuoteStore.findByQuoteId(any()) } returns null
    assertThrows<BadRequestException> {
      calculator.calculateFromQuote("id", assetService.getAsset("USDC"), "100")
    }
  }

  @Test
  fun `test calculateFromQuote with mismatched sell amount`() {
    val quoteId = "id"
    every { sep38QuoteStore.findByQuoteId(quoteId) } returns usdcQuote
    assertThrows<BadRequestException> {
      calculator.calculateFromQuote(quoteId, assetService.getAsset("USDC"), "99")
    }
  }

  @Test
  fun `test calculateFromQuote with mismatched sell asset`() {
    val quoteId = "id"
    every { sep38QuoteStore.findByQuoteId(quoteId) } returns usdcQuote
    assertThrows<BadRequestException> {
      calculator.calculateFromQuote(quoteId, assetService.getAsset("JPYC"), "100")
    }
  }

  @Test
  fun `test calculateFromQuote with bad quote`() {
    val quoteId = "id"
    every { sep38QuoteStore.findByQuoteId(quoteId) } returns usdcQuote.apply { fee = null }
    assertThrows<SepValidationException> {
      calculator.calculateFromQuote(quoteId, assetService.getAsset("USDC"), "100")
    }
  }

  @Test
  fun `test calculate`() {
    val result =
      calculator.calculate(
        assetService.getAssetByName("iso4217:USD"),
        assetService.getAsset(TEST_ASSET),
        "100",
        TEST_CUSTOMER_ID,
        token
      )
    assertEquals(
      Amounts.builder()
        .amountIn("100")
        .amountInAsset(TEST_ASSET_SEP38_FORMAT)
        .amountOut("98")
        .amountOutAsset("iso4217:USD")
        .amountFee("2")
        .amountFeeAsset("iso4217:USD")
        .build(),
      result
    )
  }

  @Test
  fun `test calculate with client set by domain`() {
    every { clientsConfig.getClientConfigBySigningKey(any()) } returns null

    calculator.calculate(
      assetService.getAssetByName("iso4217:USD"),
      assetService.getAsset(TEST_ASSET),
      "100",
      TEST_CUSTOMER_ID,
      token
    )

    verify(exactly = 1) {
      feeIntegration.getFee(
        GetFeeRequest.builder()
          .sendAmount("100")
          .sendAsset(TEST_ASSET_SEP38_FORMAT)
          .receiveAsset("iso4217:USD")
          .senderId(TEST_CUSTOMER_ID)
          .receiverId(TEST_CUSTOMER_ID)
          .clientId(clientConfig.name)
          .build()
      )
    }
  }

  @Test
  fun `test calculate with client set by signing key`() {
    every { clientsConfig.getClientConfigByDomain(any()) } returns null

    calculator.calculate(
      assetService.getAssetByName("iso4217:USD"),
      assetService.getAsset(TEST_ASSET),
      "100",
      TEST_CUSTOMER_ID,
      token
    )

    verify(exactly = 1) {
      feeIntegration.getFee(
        GetFeeRequest.builder()
          .sendAmount("100")
          .sendAsset(TEST_ASSET_SEP38_FORMAT)
          .receiveAsset("iso4217:USD")
          .senderId(TEST_CUSTOMER_ID)
          .receiverId(TEST_CUSTOMER_ID)
          .clientId(clientConfig.name)
          .build()
      )
    }
  }

  @Test
  fun `test calculate with missing client`() {
    every { clientsConfig.getClientConfigByDomain(any()) } returns null
    every { clientsConfig.getClientConfigBySigningKey(any()) } returns null

    assertThrows<BadRequestException> {
      calculator.calculate(
        assetService.getAssetByName("iso4217:USD"),
        assetService.getAsset(TEST_ASSET),
        "100",
        TEST_CUSTOMER_ID,
        token
      )
    }
  }

  @Test
  fun `test calculate with invalid client domain`() {
    every { sep10Config.allowedClientDomains } returns listOf("nothing")

    assertThrows<BadRequestException> {
      calculator.calculate(
        assetService.getAssetByName("iso4217:USD"),
        assetService.getAsset(TEST_ASSET),
        "100",
        TEST_CUSTOMER_ID,
        token
      )
    }
  }

  @Test
  fun `test calculate with invalid client name`() {
    every { sep10Config.allowedClientNames } returns listOf("nothing")

    assertThrows<BadRequestException> {
      calculator.calculate(
        assetService.getAssetByName("iso4217:USD"),
        assetService.getAsset(TEST_ASSET),
        "100",
        TEST_CUSTOMER_ID,
        token
      )
    }
  }

  @Test
  fun `test calculate with all domains allowed`() {
    every { sep10Config.allowedClientDomains } returns emptyList()

    calculator.calculate(
      assetService.getAssetByName("iso4217:USD"),
      assetService.getAsset(TEST_ASSET),
      "100",
      TEST_CUSTOMER_ID,
      token
    )

    verify(exactly = 1) {
      feeIntegration.getFee(
        GetFeeRequest.builder()
          .sendAmount("100")
          .sendAsset(TEST_ASSET_SEP38_FORMAT)
          .receiveAsset("iso4217:USD")
          .senderId(TEST_CUSTOMER_ID)
          .receiverId(TEST_CUSTOMER_ID)
          .clientId(clientConfig.name)
          .build()
      )
    }
  }

  @Test
  fun `test calculate with all names allowed`() {
    every { sep10Config.allowedClientNames } returns emptyList()

    calculator.calculate(
      assetService.getAssetByName("iso4217:USD"),
      assetService.getAsset(TEST_ASSET),
      "100",
      TEST_CUSTOMER_ID,
      token
    )

    verify(exactly = 1) {
      feeIntegration.getFee(
        GetFeeRequest.builder()
          .sendAmount("100")
          .sendAsset(TEST_ASSET_SEP38_FORMAT)
          .receiveAsset("iso4217:USD")
          .senderId(TEST_CUSTOMER_ID)
          .receiverId(TEST_CUSTOMER_ID)
          .clientId(clientConfig.name)
          .build()
      )
    }
  }

  @Test
  fun `test calculate with client attribution disabled and missing client`() {
    every { sep10Config.isClientAttributionRequired } returns false
    every { clientsConfig.getClientConfigByDomain(any()) } returns null
    every { clientsConfig.getClientConfigBySigningKey(any()) } returns null
    every {
      feeIntegration.getFee(
        GetFeeRequest.builder()
          .sendAmount("100")
          .sendAsset(TEST_ASSET_SEP38_FORMAT)
          .receiveAsset("iso4217:USD")
          .senderId(TEST_CUSTOMER_ID)
          .receiverId(TEST_CUSTOMER_ID)
          .clientId(null)
          .build()
      )
    } returns GetFeeResponse(Amount("2", "iso4217:USD"))

    calculator.calculate(
      assetService.getAssetByName("iso4217:USD"),
      assetService.getAsset(TEST_ASSET),
      "100",
      TEST_CUSTOMER_ID,
      token
    )

    verify(exactly = 1) {
      feeIntegration.getFee(
        GetFeeRequest.builder()
          .sendAmount("100")
          .sendAsset(TEST_ASSET_SEP38_FORMAT)
          .receiveAsset("iso4217:USD")
          .senderId(TEST_CUSTOMER_ID)
          .receiverId(TEST_CUSTOMER_ID)
          .clientId(null)
          .build()
      )
    }
  }

  @Test
  fun `test calculate with client attribution disabled and found client name`() {
    every { sep10Config.isClientAttributionRequired } returns false
    every { clientsConfig.getClientConfigByDomain(any()) } returns null
    every { clientsConfig.getClientConfigBySigningKey(token.account) } returns clientConfig
    every {
      feeIntegration.getFee(
        GetFeeRequest.builder()
          .sendAmount("100")
          .sendAsset(TEST_ASSET_SEP38_FORMAT)
          .receiveAsset("iso4217:USD")
          .senderId(TEST_CUSTOMER_ID)
          .receiverId(TEST_CUSTOMER_ID)
          .clientId(clientConfig.name)
          .build()
      )
    } returns GetFeeResponse(Amount("2", "iso4217:USD"))

    calculator.calculate(
      assetService.getAssetByName("iso4217:USD"),
      assetService.getAsset(TEST_ASSET),
      "100",
      TEST_CUSTOMER_ID,
      token
    )

    verify(exactly = 1) {
      feeIntegration.getFee(
        GetFeeRequest.builder()
          .sendAmount("100")
          .sendAsset(TEST_ASSET_SEP38_FORMAT)
          .receiveAsset("iso4217:USD")
          .senderId(TEST_CUSTOMER_ID)
          .receiverId(TEST_CUSTOMER_ID)
          .clientId(clientConfig.name)
          .build()
      )
    }
  }

  @Test
  fun `test calculate with client attribution disabled and found client domain`() {
    every { sep10Config.isClientAttributionRequired } returns false
    every { clientsConfig.getClientConfigByDomain(token.clientDomain) } returns clientConfig
    every { clientsConfig.getClientConfigBySigningKey(any()) } returns null
    every {
      feeIntegration.getFee(
        GetFeeRequest.builder()
          .sendAmount("100")
          .sendAsset(TEST_ASSET_SEP38_FORMAT)
          .receiveAsset("iso4217:USD")
          .senderId(TEST_CUSTOMER_ID)
          .receiverId(TEST_CUSTOMER_ID)
          .clientId(clientConfig.name)
          .build()
      )
    } returns GetFeeResponse(Amount("2", "iso4217:USD"))

    calculator.calculate(
      assetService.getAssetByName("iso4217:USD"),
      assetService.getAsset(TEST_ASSET),
      "100",
      TEST_CUSTOMER_ID,
      token
    )

    verify(exactly = 1) {
      feeIntegration.getFee(
        GetFeeRequest.builder()
          .sendAmount("100")
          .sendAsset(TEST_ASSET_SEP38_FORMAT)
          .receiveAsset("iso4217:USD")
          .senderId(TEST_CUSTOMER_ID)
          .receiverId(TEST_CUSTOMER_ID)
          .clientId(clientConfig.name)
          .build()
      )
    }
  }
}
