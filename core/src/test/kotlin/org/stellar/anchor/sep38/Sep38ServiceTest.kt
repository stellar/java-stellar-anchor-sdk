package org.stellar.anchor.sep38

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.asset.AssetInfo
import org.stellar.anchor.asset.ResourceJsonAssetService
import org.stellar.anchor.config.Sep38Config
import org.stellar.anchor.dto.sep38.GetPricesResponse
import org.stellar.anchor.dto.sep38.InfoResponse
import org.stellar.anchor.exception.AnchorException
import org.stellar.anchor.exception.BadRequestException
import org.stellar.anchor.exception.NotFoundException
import org.stellar.anchor.exception.ServerErrorException
import org.stellar.anchor.integration.rate.GetRateRequest
import org.stellar.anchor.integration.rate.GetRateResponse

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
  fun test_validateAssetWithMsgPrefix() {
    // empty sell_asset
    var ex: AnchorException = assertThrows {
      sep38Service.validateAssetWithMsgPrefix("sell_", null, null, null, null)
    }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_asset cannot be empty", ex.message)

    // nonexistent sell_asset
    ex =
      assertThrows { sep38Service.validateAssetWithMsgPrefix("sell_", "foo:bar", null, null, null) }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("sell_asset not found", ex.message)

    // empty sell_amount
    ex =
      assertThrows {
        sep38Service.validateAssetWithMsgPrefix("sell_", "iso4217:USD", null, null, null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount cannot be empty", ex.message)

    // invalid (not a number) sell_amount
    ex =
      assertThrows {
        sep38Service.validateAssetWithMsgPrefix("sell_", "iso4217:USD", "foo", null, null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount is invalid", ex.message)

    // sell_amount should be positive
    ex =
      assertThrows {
        sep38Service.validateAssetWithMsgPrefix("sell_", "iso4217:USD", "-0.01", null, null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount should be positive", ex.message)

    // sell_amount should be positive
    ex =
      assertThrows {
        sep38Service.validateAssetWithMsgPrefix("buy_", "iso4217:USD", "0", null, null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("buy_amount should be positive", ex.message)

    // country_code, sell_delivery_method and buy_delivery_method are not mandatory
    assertDoesNotThrow {
      sep38Service.validateAssetWithMsgPrefix("sell_", "iso4217:USD", "1.23", null, null)
    }

    // unsupported sell_delivery_method
    ex =
      assertThrows {
        sep38Service.validateAssetWithMsgPrefix("sell_", "iso4217:USD", "1.23", "FOO", null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported sell delivery method", ex.message)

    // unsupported buy_delivery_method
    ex =
      assertThrows {
        sep38Service.validateAssetWithMsgPrefix("sell_", "iso4217:USD", "1.23", "WIRE", "BAR")
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported buy delivery method", ex.message)

    // success
    assertDoesNotThrow {
      sep38Service.validateAssetWithMsgPrefix("sell_", "iso4217:USD", "1.23", "WIRE", "WIRE")
    }
  }

  @Test
  fun test_getPrices_failure() {
    // empty rateIntegration should throw an error
    var ex: AnchorException = assertThrows { sep38Service.getPrices(null, null, null, null, null) }
    var wantException: AnchorException = ServerErrorException("internal server error")
    assertEquals(wantException, ex)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, mockRateIntegration)

    // test if input is being validated
    ex = assertThrows { sep38Service.getPrices(null, null, null, null, null) }
    wantException = BadRequestException("sell_asset cannot be empty")
    assertEquals(wantException, ex)

    // test if country_code is being validated
    ex = assertThrows { sep38Service.getPrices("iso4217:USD", "1.23", null, null, "FOO") }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported country code", ex.message)
  }

  @Test
  fun test_getPrices_minimumParameters() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq1 =
      GetRateRequest.builder()
        .sellAsset("iso4217:USD")
        .buyAsset("stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
        .sellAmount("100")
        .build()
    every { mockRateIntegration.getRate(getRateReq1) } returns GetRateResponse("1")
    val getRateReq2 =
      GetRateRequest.builder()
        .sellAsset("iso4217:USD")
        .buyAsset("stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
        .sellAmount("100")
        .build()
    every { mockRateIntegration.getRate(getRateReq2) } returns GetRateResponse("2")
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, mockRateIntegration)

    // test happy path with the minimum parameters
    var gotResponse: GetPricesResponse? = null
    assertDoesNotThrow {
      gotResponse = sep38Service.getPrices("iso4217:USD", "100", null, null, null)
    }
    val wantResponse = GetPricesResponse()
    wantResponse.addAsset(
      "stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "1"
    )
    wantResponse.addAsset(
      "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "2"
    )
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun test_getPrices_allParameters() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq1 =
      GetRateRequest.builder()
        .sellAsset("iso4217:USD")
        .buyAsset("stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
        .sellAmount("100")
        .countryCode("USA")
        .sellDeliveryMethod("WIRE")
        .build()
    every { mockRateIntegration.getRate(getRateReq1) } returns GetRateResponse("1.1")
    val getRateReq2 =
      GetRateRequest.builder()
        .sellAsset("iso4217:USD")
        .buyAsset("stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
        .sellAmount("100")
        .countryCode("USA")
        .sellDeliveryMethod("WIRE")
        .build()
    every { mockRateIntegration.getRate(getRateReq2) } returns GetRateResponse("2.1")
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, mockRateIntegration)

    // test happy path with all the parameters
    var gotResponse: GetPricesResponse? = null
    assertDoesNotThrow {
      gotResponse = sep38Service.getPrices("iso4217:USD", "100", "WIRE", null, "USA")
    }
    val wantResponse = GetPricesResponse()
    wantResponse.addAsset(
      "stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "1.1"
    )
    wantResponse.addAsset(
      "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "2.1"
    )
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun test_getPrices_filterWithBuyDeliveryMethod() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq1 =
      GetRateRequest.builder()
        .sellAsset("stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
        .buyAsset("iso4217:USD")
        .sellAmount("100")
        .buyDeliveryMethod("WIRE")
        .build()
    every { mockRateIntegration.getRate(getRateReq1) } returns GetRateResponse("1")
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, mockRateIntegration)

    // test happy path with the minimum parameters and specify buy_delivery_method
    val sellAssetName = "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    var gotResponse: GetPricesResponse? = null
    assertDoesNotThrow {
      gotResponse = sep38Service.getPrices(sellAssetName, "100", null, "WIRE", null)
    }
    val wantResponse = GetPricesResponse()
    wantResponse.addAsset("iso4217:USD", "1")
    assertEquals(wantResponse, gotResponse)
  }
}
