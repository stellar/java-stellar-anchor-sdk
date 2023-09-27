package org.stellar.anchor.sep6

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.TestConstants.Companion.TEST_ACCOUNT
import org.stellar.anchor.TestConstants.Companion.TEST_ASSET
import org.stellar.anchor.TestConstants.Companion.TEST_ASSET_SEP38_FORMAT
import org.stellar.anchor.api.callback.FeeIntegration
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.api.callback.GetFeeResponse
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.SepValidationException
import org.stellar.anchor.api.sep.sep38.RateFee
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.sep38.PojoSep38Quote
import org.stellar.anchor.sep38.Sep38QuoteStore
import org.stellar.anchor.sep6.ExchangeAmountsCalculator.Amounts

class ExchangeAmountsCalculatorTest {
  private val assetService: AssetService = DefaultAssetService.fromJsonResource("test_assets.json")

  @MockK(relaxed = true) lateinit var feeIntegration: FeeIntegration
  @MockK(relaxed = true) lateinit var sep38QuoteStore: Sep38QuoteStore

  private lateinit var calculator: ExchangeAmountsCalculator

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    calculator = ExchangeAmountsCalculator(feeIntegration, sep38QuoteStore, assetService)
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
    every {
      feeIntegration.getFee(
        GetFeeRequest.builder()
          .sendAmount("100")
          .sendAsset(TEST_ASSET_SEP38_FORMAT)
          .receiveAsset("iso4217:USD")
          .senderId(TEST_ACCOUNT)
          .receiverId(TEST_ACCOUNT)
          .clientId(TEST_ACCOUNT)
          .build()
      )
    } returns GetFeeResponse(Amount("2", "iso4217:USD"))

    val result =
      calculator.calculate(
        assetService.getAssetByName("iso4217:USD"),
        assetService.getAsset(TEST_ASSET),
        "100",
        TEST_ACCOUNT
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
}
