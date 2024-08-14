package org.stellar.anchor.platform.callback

import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.api.callback.GetRateRequest
import org.stellar.anchor.api.callback.GetRateRequest.Type.from
import org.stellar.anchor.api.callback.GetRateResponse
import org.stellar.anchor.api.exception.ServerErrorException
import org.stellar.anchor.api.sep.AssetInfo
import org.stellar.anchor.api.sep.AssetInfo.Schema.iso4217
import org.stellar.anchor.api.sep.AssetInfo.Schema.stellar
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.util.GsonUtils

class RestRateIntegrationTest {
  private val assetService = mockk<AssetService>()
  private val gson: Gson = GsonUtils.getInstance()
  private var usdAssetInfo = AssetInfo()
  private var usdcAssetInfo = AssetInfo()
  private val rateIntegration = RestRateIntegration("/", null, null, gson, assetService)
  private var request: GetRateRequest = GetRateRequest()
  private var rateResponse: GetRateResponse = GetRateResponse()

  @BeforeEach
  fun setUp() {
    // Set up USDC asset info
    usdAssetInfo.code = "USD"
    usdAssetInfo.schema = iso4217
    usdAssetInfo.significantDecimals = 2

    // Set up USD asset info
    usdcAssetInfo.schema = stellar
    usdcAssetInfo.code = "USDC"
    usdcAssetInfo.issuer = "GABCD"
    usdcAssetInfo.significantDecimals = 2

    // Set up asset service
    every { assetService.getAssetByName("iso4217:USD") } returns usdAssetInfo
    every { assetService.getAssetByName("stellar:USDC:GABCD") } returns usdcAssetInfo
    every { assetService.getAssetByName("unknown") } returns null

    // Set up request and response
    request =
      gson.fromJson(
        """
        {
          "type": "indicative",
          "sell_asset": "iso4217:USD",
          "sell_amount": "106",
          "buy_asset": "stellar:USDC:GABCD"
        }
        """,
        GetRateRequest::class.java,
      )

    rateResponse =
      gson.fromJson(
        """
        {
          "rate": {
            "id": 1,
            "price": "1.05",
            "sell_amount": "100",
            "buy_amount": "94.29",
            "fee": {
              "total": "1.00",
              "asset": "iso4217:USD",
              "details": [
                {
                  "name": "Sell fee #1",
                  "description": "Fee related to selling the asset #1.",
                  "amount": "0.70"
                },
                {
                  "name": "Sell fee #2",
                  "description": "Fee related to selling the asset #2.",
                  "amount": "0.30"
                }
              ]
            }
          }
        }
        """,
        GetRateResponse::class.java,
      )
  }

  @ParameterizedTest
  @ValueSource(strings = ["indicative", "firm"])
  fun `test INDICATIVE and FIRM validateRateResponse`(type: String) {
    request.type = from(type)
    rateResponse.rate.id = "1234"
    rateResponse.rate.expiresAt = Instant.now()

    // The fee is in sell_asset
    rateResponse.rate.fee.asset = usdAssetInfo.sep38AssetName
    rateResponse.rate.sellAmount = "100.00"
    rateResponse.rate.buyAmount = "94.29"
    rateIntegration.validateRateResponse(request, rateResponse)

    // The fee is in buy_asset
    rateResponse.rate.fee.asset = usdcAssetInfo.sep38AssetName
    rateResponse.rate.sellAmount = "100.00"
    rateResponse.rate.buyAmount = "94.24"
    rateIntegration.validateRateResponse(request, rateResponse)
  }

  @ParameterizedTest
  @ValueSource(strings = ["-1.0", "-2000"])
  @NullSource
  fun `test bad sell and buy amounts`(badAmount: String?) {
    // Bad sell amount
    rateResponse.rate.sellAmount = badAmount
    rateResponse.rate.buyAmount = "94.29"
    var ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'sell_amount' is missing or not a positive number in the GET /rate response",
      ex.message,
    )

    // Bad buy amount
    rateResponse.rate.sellAmount = "100"
    rateResponse.rate.buyAmount = badAmount
    ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'buy_amount' is missing or not a positive number in the GET /rate response",
      ex.message,
    )
  }

  @Test
  fun `test mis-matched sell_amount and buy_amount`() {
    // Bad sell amount
    rateResponse.rate.sellAmount = "100.01"
    var ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'sell_amount' is not equal to price * buy_amount + (fee?:0) in the GET /rate response",
      ex.message,
    )

    rateResponse.rate.sellAmount = "100.00"
    rateResponse.rate.buyAmount = "94.00"

    ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'sell_amount' is not equal to price * buy_amount + (fee?:0) in the GET /rate response",
      ex.message,
    )
  }

  @Test
  fun `test bad fee total and asset`() {
    rateResponse.rate.fee.total = null
    var ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'fee.total' is missing or not a positive number in the GET /rate response",
      ex.message,
    )

    rateResponse.rate.fee.total = "1.00"
    rateResponse.rate.fee.asset = "unknown"
    ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'fee.asset' is missing or not a valid asset in the GET /rate response",
      ex.message,
    )
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1])
  fun `test bad fee details without name or amount`(feeIndex: Int) {
    rateResponse.rate.fee.details[feeIndex].name = null
    var ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'fee.details.description[?].name' is missing in the GET /rate response",
      ex.message,
    )

    rateResponse.rate.fee.details[feeIndex].name = "Sell fee"
    rateResponse.rate.fee.details[feeIndex].amount = null
    ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'fee.details[?].description.amount' is missing or not a positive number in the GET /rate response",
      ex.message,
    )

    rateResponse.rate.fee.details[feeIndex].name = "Sell fee"
    rateResponse.rate.fee.details[feeIndex].amount = "0.71"
    ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'sell_amount' is not equal to price * buy_amount + (fee ?: 0) to  in the GET /rate response",
      ex.message,
    )
  }
}
