package org.stellar.anchor.platform.callback

import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.callback.GetRateRequest
import org.stellar.anchor.api.callback.GetRateResponse
import org.stellar.anchor.api.sep.AssetInfo
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.util.GsonUtils

class RestRateIntegrationTest {

  private val assetService = mockk<AssetService>()
  private val gson: Gson = GsonUtils.getInstance()
  private var usdAssetInfo = AssetInfo()
  private var usdcAssetInfo = AssetInfo()
  private val rateIntegration = RestRateIntegration("/", null, null, gson, assetService)

  @BeforeEach
  fun setUp() {
    // Set up USDC asset info
    usdAssetInfo.code = "USD"
    usdAssetInfo.issuer = "GABCD"
    usdAssetInfo.significantDecimals = 2

    // Set up USD asset info
    usdcAssetInfo.code = "USDC"
    usdcAssetInfo.issuer = "GABCD"
    usdAssetInfo.significantDecimals = 2

    // Set up asset service
    every { assetService.getAsset("USD") } returns usdAssetInfo
    every { assetService.getAsset("USDC") } returns usdAssetInfo
  }

  @Test
  fun `test validateRateResponse`() {
    val request =
      gson.fromJson(
        """
        {
          "type": "INDICATIVE",
          "sell_asset": "USD",
          "sell_amount": "106",
          "buy_asset": "USDC"
        }
        """,
        GetRateRequest::class.java,
      )

    val rateResponse =
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
              "asset": "USD",
              "details": [
                {
                  "name": "Sell fee",
                  "description": "Fee related to selling the asset.",
                  "amount": "1.00"
                }
              ]
            }
          }
        }
        """,
        GetRateResponse::class.java,
      )
    rateIntegration.validateRateResponse(request, rateResponse)
  }
}
