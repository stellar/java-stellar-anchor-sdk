package org.stellar.anchor.sep38

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.asset.AssetResponse
import org.stellar.anchor.asset.ResourceJsonAssetService
import org.stellar.anchor.config.Sep38Config
import org.stellar.anchor.dto.sep38.InfoResponse

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
    sep38Service = Sep38Service(sep8Config, assetService)
    assertEquals(3, assets.size)
  }

  @Test
  fun test_getInfo() {
    val infoResponse = sep38Service.getInfo()
    assertEquals(3, infoResponse.assets.size)

    val assetMap = HashMap<String, InfoResponse.AssetInfo>()
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
      AssetResponse.Sep38Operation.DeliveryMethod(
        "WIRE",
        "Send USD directly to the Anchor's bank account."
      )
    assertEquals(listOf(wantSellDeliveryMethod), fiatUSD.sellDeliveryMethods)
    val wantBuyDeliveryMethod =
      AssetResponse.Sep38Operation.DeliveryMethod(
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
}
