package org.stellar.anchor.platform.callback

import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.api.callback.GetRateRequest
import org.stellar.anchor.api.callback.GetRateRequest.Type.from
import org.stellar.anchor.api.callback.GetRateResponse
import org.stellar.anchor.api.exception.ServerErrorException
import org.stellar.anchor.api.sep.AssetInfo
import org.stellar.anchor.api.sep.AssetInfo.Schema.ISO_4217
import org.stellar.anchor.api.sep.AssetInfo.Schema.STELLAR
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.platform.callback.RestRateIntegration.withinRoundingError
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
    usdAssetInfo.schema = ISO_4217
    usdAssetInfo.significantDecimals = 2

    // Set up USD asset info
    usdcAssetInfo.schema = STELLAR
    usdcAssetInfo.code = "USDC"
    usdcAssetInfo.issuer = "GABCD"
    usdcAssetInfo.significantDecimals = 7

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
  @CsvSource(
    value =
      [
        "-1.0,is missing or not a positive number in the GET /rate response",
        "-1.00000001,is missing or not a positive number in the GET /rate response",
        "-2000,is missing or not a positive number in the GET /rate response",
        "null,is missing or not a positive number in the GET /rate response",
        "1.00000001,has incorrect number of significant decimals",
      ]
  )
  fun `test bad sell and buy amounts`(badAmount: String?, errorMessage: String) {
    // Bad sell amount
    rateResponse.rate.sellAmount = badAmount
    rateResponse.rate.buyAmount = "94.29"
    var ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    //    assertEquals("'rate.sell_amount' ${errorMessage.trim()}", ex.message)
    assertTrue(ex.message!!.contains(errorMessage))

    // Bad buy amount
    rateResponse.rate.sellAmount = "100"
    rateResponse.rate.buyAmount = badAmount
    ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertTrue(ex.message!!.contains(errorMessage))
  }

  @Test
  fun `test mis-matched sell_amount and buy_amount`() {
    // Bad sell amount
    rateResponse.rate.sellAmount = "100.02" // expect 100.00
    rateResponse.rate.buyAmount = "94.29"
    var ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'rate.sell_amount' (100.02) is not within rounding error of the expected (100.0045) ('price * buy_amount + fee') in the GET /rate response",
      ex.message,
    )

    rateResponse.rate.sellAmount = "100.00"
    rateResponse.rate.buyAmount = "94.00"

    ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'rate.sell_amount' (100.00) is not within rounding error of the expected (99.7000) ('price * buy_amount + fee') in the GET /rate response",
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
      "'rate.fee.total' is missing or not a positive number in the GET /rate response",
      ex.message,
    )

    rateResponse.rate.fee.total = "1.00"
    rateResponse.rate.fee.asset = "unknown"
    ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'rate.fee.asset' is missing or not a valid asset in the GET /rate response",
      ex.message,
    )

    rateResponse.rate.fee.total = "1.00000001"
    rateResponse.rate.fee.asset = "iso4217:USD"
    ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'rate.fee.total' (1.00000001) has incorrect number of significant decimals (expected: 2) in the GET /rate response",
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
      "'rate.fee.details.description[?].name' is missing in the GET /rate response",
      ex.message,
    )

    rateResponse.rate.fee.details[feeIndex].name = "Sell fee"
    rateResponse.rate.fee.details[feeIndex].amount = null
    ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'rate.fee.details[?].description.amount' is missing or not a positive number in the GET /rate response",
      ex.message,
    )

    rateResponse.rate.fee.details[feeIndex].name = "Sell fee"
    rateResponse.rate.fee.details[feeIndex].amount = "0.71" // Expect 0.70
    ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertTrue(ex.message!!.contains("is not equal to the sum of fees "))
  }

  @Test
  fun `test bad decimals from the response`() {
    // Bad sell amount
    rateResponse.rate.sellAmount = "100.00001234"
    rateResponse.rate.buyAmount = "94.29"
    rateResponse.rate.fee.details[0].amount = "0.7"
    var ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'rate.sell_amount' (100.00001234) has incorrect number of significant decimals (expected: 2) in the GET /rate response",
      ex.message,
    )

    // Bad buy amount
    rateResponse.rate.sellAmount = "100"
    rateResponse.rate.buyAmount = "94.000000029"
    rateResponse.rate.fee.details[0].amount = "0.7"
    ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'rate.buy_amount' (94.000000029) has incorrect number of significant decimals (expected: 7) in the GET /rate response",
      ex.message,
    )

    // Bad fee amount
    rateResponse.rate.sellAmount = "100"
    rateResponse.rate.buyAmount = "94.29"
    rateResponse.rate.fee.details[0].amount = "0.00007"
    ex =
      assertThrows<ServerErrorException> {
        rateIntegration.validateRateResponse(request, rateResponse)
      }
    assertEquals(
      "'rate.fee.details[?].description.amount' has incorrect number of significant decimals in the GET /rate response",
      ex.message,
    )
  }

  @ParameterizedTest
  @CsvSource(
    value =
      [
        // Round down
        "1.014,1.01,2,true",
        "1.014, 1.01,3,false",
        "1.0104, 1.01,3,true",
        // Round up
        "1.014,1.02,2,true",
        // Ceil
        "1.001, 1.01,2,true",
        "1.001,1.01, 3, false",
        // Floor
        "1.019, 1.01, 2, true",
        "1.019, 1.01, 3, false",

        // sell has 2, buy has 5. calculated sell amount is 4.99999. returned sell amount is
        // 5.00
        "4.99999,5.00,2,true",
        "4.99999,5.00,5,false",

        // sell has 2, buy has 5. calculated sell amount is 4.99999. returned sell amount is
        // 5.00
        "4.99999,5.00,2,true",
        "4.99999,5.00,5,false",

        // Test cases
        "4.99999999984375,5,2,true",
        "4.99999999984375,5,3,true",
        "4.99999999984375,5,4,true",
        "4.99999999984375,5,5,true",
        "4.99999999984375,5,8,true",
        "4.99999999984375,5,9,true",
        "4.99999999984375,5,10,false",
      ]
  )
  fun `test equals in scale withing rounding errors`(
    amount: String,
    expected: String,
    scale: String,
    shouldAllow: String,
  ) {
    assertEquals(
      shouldAllow.toBoolean(),
      withinRoundingError(BigDecimal(amount), BigDecimal(expected), scale.toInt()),
    )
  }
}
