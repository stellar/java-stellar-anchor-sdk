package org.stellar.anchor.sep38

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.asset.AssetInfo
import org.stellar.anchor.asset.ResourceJsonAssetService
import org.stellar.anchor.config.Sep38Config
import org.stellar.anchor.dto.sep38.InfoResponse
import org.stellar.anchor.exception.HttpException

class Sep38ServiceTest {
  internal class PropertySep38Config : Sep38Config {
    override fun isEnabled(): Boolean {
      return true
    }

    override fun getQuoteIntegrationEndPoint(): String? {
      return null
    }
  }

  private lateinit var sep38Service: Sep38Service

  @BeforeEach
  fun setUp() {
    val assetService = ResourceJsonAssetService("test_assets.json")
    val assets = assetService.listAllAssets()
    val sep8Config = PropertySep38Config()
    this.sep38Service = Sep38Service(sep8Config, assetService, null)
    assertEquals(3, assets.size)
  }

  @Test
  fun test_getInfo() {
    val infoResponse = sep38Service.getInfo()
    assertEquals(3, infoResponse.assets.size)

    val assetMap = HashMap<String, InfoResponse.Asset>()
    infoResponse.assets.forEach { assetMap[it.asset] = it }
    assertEquals(3, assetMap.size)

    val stellarUSDC =
      assetMap["stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"]
    assertNotNull(stellarUSDC)
    assertNull(stellarUSDC!!.countryCodes)
    assertNull(stellarUSDC.sellDeliveryMethods)
    assertNull(stellarUSDC.buyDeliveryMethods)
    var wantAssets =
      listOf("iso4217:USD", "stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
    assertTrue(stellarUSDC.exchangeableAssetNames.containsAll(wantAssets))
    assertTrue(wantAssets.containsAll(stellarUSDC.exchangeableAssetNames))

    val stellarJPYC =
      assetMap["stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"]
    assertNotNull(stellarJPYC)
    assertNull(stellarJPYC!!.countryCodes)
    assertNull(stellarJPYC.sellDeliveryMethods)
    assertNull(stellarJPYC.buyDeliveryMethods)
    wantAssets =
      listOf("iso4217:USD", "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
    assertTrue(stellarJPYC.exchangeableAssetNames.containsAll(wantAssets))
    assertTrue(wantAssets.containsAll(stellarJPYC.exchangeableAssetNames))

    val fiatUSD = assetMap["iso4217:USD"]
    assertNotNull(fiatUSD)
    assertEquals(listOf("USA"), fiatUSD!!.countryCodes)
    val wantSellDeliveryMethod =
      AssetInfo.Sep38Operation.DeliveryMethod(
        "WIRE",
        "Send USD directly to the Anchor's bank account."
      )
    assertEquals(listOf(wantSellDeliveryMethod), fiatUSD.sellDeliveryMethods)
    val wantBuyDeliveryMethod =
      AssetInfo.Sep38Operation.DeliveryMethod(
        "WIRE",
        "Have USD sent directly to your bank account."
      )
    assertEquals(listOf(wantBuyDeliveryMethod), fiatUSD.buyDeliveryMethods)
    wantAssets =
      listOf(
        "stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
        "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      )
    assertTrue(fiatUSD.exchangeableAssetNames.containsAll(wantAssets))
    assertTrue(wantAssets.containsAll(fiatUSD.exchangeableAssetNames))
  }

  @Test
  fun test_validateGetPricesInput() {
    // empty sell_asset
    var ex: HttpException = assertThrows {
      sep38Service.validateGetPricesInput(null, null, null, null, null)
    }
    var wantException = HttpException(400, "sell_asset cannot be empty")
    assertEquals(wantException, ex)

    // nonexistent sell_asset
    ex = assertThrows { sep38Service.validateGetPricesInput("foo:bar", null, null, null, null) }
    wantException = HttpException(404, "sell_asset not found")
    assertEquals(wantException, ex)

    // empty sell_amount
    ex = assertThrows { sep38Service.validateGetPricesInput("iso4217:USD", null, null, null, null) }
    wantException = HttpException(400, "sell_amount cannot be empty")
    assertEquals(wantException, ex)

    // country_code, sell_delivery_method and buy_delivery_method are not mandatory
    assertDoesNotThrow {
      sep38Service.validateGetPricesInput("iso4217:USD", "1.23", null, null, null)
    }

    // unsupported country_code
    ex =
      assertThrows { sep38Service.validateGetPricesInput("iso4217:USD", "1.23", "BRA", null, null) }
    wantException = HttpException(400, "Unsupported country code")
    assertEquals(wantException, ex)

    // unsupported sell_delivery_method
    ex =
      assertThrows {
        sep38Service.validateGetPricesInput("iso4217:USD", "1.23", "USA", "FOO", null)
      }
    wantException = HttpException(400, "Unsupported sell delivery method")
    assertEquals(wantException, ex)

    // unsupported buy_delivery_method
    ex =
      assertThrows {
        sep38Service.validateGetPricesInput("iso4217:USD", "1.23", "USA", "WIRE", "BAR")
      }
    wantException = HttpException(400, "Unsupported buy delivery method")
    assertEquals(wantException, ex)

    // success
    assertDoesNotThrow {
      sep38Service.validateGetPricesInput("iso4217:USD", "1.23", "USA", "WIRE", "WIRE")
    }
  }
}
