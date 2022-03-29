package org.stellar.anchor.sep38

import io.mockk.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.asset.AssetInfo
import org.stellar.anchor.asset.ResourceJsonAssetService
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.config.Sep38Config
import org.stellar.anchor.dto.sep38.*
import org.stellar.anchor.exception.AnchorException
import org.stellar.anchor.exception.BadRequestException
import org.stellar.anchor.exception.NotFoundException
import org.stellar.anchor.exception.ServerErrorException
import org.stellar.anchor.integration.rate.GetRateRequest
import org.stellar.anchor.integration.rate.GetRateResponse
import org.stellar.anchor.model.Sep38Quote
import org.stellar.anchor.model.Sep38QuoteBuilder
import org.stellar.anchor.sep10.JwtToken

class Sep38ServiceTest {
  internal class PropertySep38Config : Sep38Config {
    override fun isEnabled(): Boolean {
      return true
    }

    override fun getQuoteIntegrationEndPoint(): String? {
      return null
    }
  }

  companion object {
    private const val PUBLIC_KEY = "GBJDSMTMG4YBP27ZILV665XBISBBNRP62YB7WZA2IQX2HIPK7ABLF4C2"
  }

  private lateinit var sep38Service: Sep38Service

  private val stellarUSDC = "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"

  // store/db related:
  private lateinit var quoteStore: Sep38QuoteStore

  // sep10 related:
  private lateinit var appConfig: AppConfig

  @BeforeEach
  fun setUp() {
    val assetService = ResourceJsonAssetService("test_assets.json")
    val assets = assetService.listAllAssets()
    val sep8Config = PropertySep38Config()
    this.sep38Service = Sep38Service(sep8Config, assetService, null, null)
    assertEquals(3, assets.size)

    // sep10 related:
    this.appConfig = mockk(relaxed = true)
    every { appConfig.jwtSecretKey } returns "secret"

    // store/db related:
    this.quoteStore = mockk(relaxed = true)
    every { quoteStore.newInstance() } returns PojoSep38Quote()
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun test_getInfo() {
    val infoResponse = sep38Service.getInfo()
    assertEquals(3, infoResponse.assets.size)

    val assetMap = HashMap<String, InfoResponse.Asset>()
    infoResponse.assets.forEach { assetMap[it.asset] = it }
    assertEquals(3, assetMap.size)

    val usdcAsset = assetMap[stellarUSDC]
    assertNotNull(usdcAsset)
    assertNull(usdcAsset!!.countryCodes)
    assertNull(usdcAsset.sellDeliveryMethods)
    assertNull(usdcAsset.buyDeliveryMethods)
    var wantAssets =
      listOf("iso4217:USD", "stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
    assertTrue(usdcAsset.exchangeableAssetNames.containsAll(wantAssets))
    assertTrue(wantAssets.containsAll(usdcAsset.exchangeableAssetNames))

    val stellarJPYC =
      assetMap["stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"]
    assertNotNull(stellarJPYC)
    assertNull(stellarJPYC!!.countryCodes)
    assertNull(stellarJPYC.sellDeliveryMethods)
    assertNull(stellarJPYC.buyDeliveryMethods)
    wantAssets = listOf("iso4217:USD", stellarUSDC)
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
      listOf("stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5", stellarUSDC)
    assertTrue(fiatUSD.exchangeableAssetNames.containsAll(wantAssets))
    assertTrue(wantAssets.containsAll(fiatUSD.exchangeableAssetNames))
  }

  @Test
  fun test_getPrices_failure() {
    // empty rateIntegration should throw an error
    var ex: AnchorException = assertThrows { sep38Service.getPrices(null, null, null, null, null) }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("internal server error", ex.message)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, mockRateIntegration, null)

    // empty sell_asset
    ex = assertThrows { sep38Service.getPrices(null, null, null, null, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_asset cannot be empty", ex.message)

    // nonexistent sell_asset
    ex = assertThrows { sep38Service.getPrices("foo:bar", null, null, null, null) }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("sell_asset not found", ex.message)

    // empty sell_amount
    ex = assertThrows { sep38Service.getPrices("iso4217:USD", null, null, null, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount cannot be empty", ex.message)

    // invalid (not a number) sell_amount
    ex = assertThrows { sep38Service.getPrices("iso4217:USD", "foo", null, null, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount is invalid", ex.message)

    // sell_amount should be positive
    ex = assertThrows { sep38Service.getPrices("iso4217:USD", "-0.01", null, null, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount should be positive", ex.message)

    // sell_amount should be positive
    ex = assertThrows { sep38Service.getPrices("iso4217:USD", "0", null, null, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount should be positive", ex.message)

    // unsupported sell_delivery_method
    ex = assertThrows { sep38Service.getPrices("iso4217:USD", "1.23", "FOO", null, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported sell delivery method", ex.message)

    // unsupported country_code
    ex = assertThrows { sep38Service.getPrices("iso4217:USD", "1.23", "WIRE", null, "FOO") }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported country code", ex.message)
  }

  @Test
  fun test_getPrices_minimumParameters() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq1 =
      GetRateRequest.builder()
        .type(GetRateRequest.Type.INDICATIVE)
        .sellAsset("iso4217:USD")
        .buyAsset("stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
        .sellAmount("100")
        .build()
    every { mockRateIntegration.getRate(getRateReq1) } returns GetRateResponse("1")
    val getRateReq2 =
      GetRateRequest.builder()
        .type(GetRateRequest.Type.INDICATIVE)
        .sellAsset("iso4217:USD")
        .buyAsset(stellarUSDC)
        .sellAmount("100")
        .build()
    every { mockRateIntegration.getRate(getRateReq2) } returns GetRateResponse("2")
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, mockRateIntegration, null)

    // test happy path with the minimum parameters
    var gotResponse: GetPricesResponse? = null
    assertDoesNotThrow {
      gotResponse = sep38Service.getPrices("iso4217:USD", "100", null, null, null)
    }
    val wantResponse = GetPricesResponse()
    wantResponse.addAsset(
      "stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      7,
      "1"
    )
    wantResponse.addAsset(stellarUSDC, 7, "2")
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun test_getPrices_allParameters() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq1 =
      GetRateRequest.builder()
        .type(GetRateRequest.Type.INDICATIVE)
        .sellAsset("iso4217:USD")
        .buyAsset("stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
        .sellAmount("100")
        .countryCode("USA")
        .sellDeliveryMethod("WIRE")
        .build()
    every { mockRateIntegration.getRate(getRateReq1) } returns GetRateResponse("1.1")
    val getRateReq2 =
      GetRateRequest.builder()
        .type(GetRateRequest.Type.INDICATIVE)
        .sellAsset("iso4217:USD")
        .buyAsset(stellarUSDC)
        .sellAmount("100")
        .countryCode("USA")
        .sellDeliveryMethod("WIRE")
        .build()
    every { mockRateIntegration.getRate(getRateReq2) } returns GetRateResponse("2.1")
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, mockRateIntegration, null)

    // test happy path with all the parameters
    var gotResponse: GetPricesResponse? = null
    assertDoesNotThrow {
      gotResponse = sep38Service.getPrices("iso4217:USD", "100", "WIRE", null, "USA")
    }
    val wantResponse = GetPricesResponse()
    wantResponse.addAsset(
      "stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      7,
      "1.1"
    )
    wantResponse.addAsset(stellarUSDC, 7, "2.1")
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun test_getPrices_filterWithBuyDeliveryMethod() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq1 =
      GetRateRequest.builder()
        .type(GetRateRequest.Type.INDICATIVE)
        .sellAsset(stellarUSDC)
        .buyAsset("iso4217:USD")
        .sellAmount("100")
        .buyDeliveryMethod("WIRE")
        .build()
    every { mockRateIntegration.getRate(getRateReq1) } returns GetRateResponse("1")
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, mockRateIntegration, null)

    // test happy path with the minimum parameters and specify buy_delivery_method
    var gotResponse: GetPricesResponse? = null
    assertDoesNotThrow {
      gotResponse = sep38Service.getPrices(stellarUSDC, "100", null, "WIRE", null)
    }
    val wantResponse = GetPricesResponse()
    wantResponse.addAsset("iso4217:USD", 4, "1")
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun test_getPrice_failure() {
    // empty rateIntegration should throw an error
    var ex: AnchorException = assertThrows {
      sep38Service.getPrice(null, null, null, null, null, null, null)
    }
    var wantException: AnchorException = ServerErrorException("internal server error")
    assertEquals(wantException, ex)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, mockRateIntegration, null)

    // empty sell_asset
    ex = assertThrows { sep38Service.getPrice(null, null, null, null, null, null, null) }
    wantException = BadRequestException("sell_asset cannot be empty")
    assertEquals(wantException, ex)

    // nonexistent sell_asset
    ex = assertThrows { sep38Service.getPrice("foo:bar", null, null, null, null, null, null) }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("sell_asset not found", ex.message)

    // empty buy_asset
    ex = assertThrows { sep38Service.getPrice("iso4217:USD", null, null, null, null, null, null) }
    wantException = BadRequestException("buy_asset cannot be empty")
    assertEquals(wantException, ex)

    // nonexistent buy_asset
    ex =
      assertThrows { sep38Service.getPrice("iso4217:USD", null, null, "foo:bar", null, null, null) }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("buy_asset not found", ex.message)

    // both sell_amount & buy_amount are empty
    ex =
      assertThrows {
        sep38Service.getPrice("iso4217:USD", null, null, stellarUSDC, null, null, null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Please provide either sell_amount or buy_amount", ex.message)

    // both sell_amount & buy_amount are filled
    ex =
      assertThrows {
        sep38Service.getPrice("iso4217:USD", "100", null, stellarUSDC, "100", null, null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Please provide either sell_amount or buy_amount", ex.message)

    // invalid (not a number) sell_amount
    ex =
      assertThrows {
        sep38Service.getPrice("iso4217:USD", "foo", null, stellarUSDC, null, null, null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount is invalid", ex.message)

    // sell_amount should be positive
    ex =
      assertThrows {
        sep38Service.getPrice("iso4217:USD", "-0.01", null, stellarUSDC, null, null, null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount should be positive", ex.message)

    // sell_amount should be positive
    ex =
      assertThrows {
        sep38Service.getPrice("iso4217:USD", "0", null, stellarUSDC, null, null, null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount should be positive", ex.message)

    // invalid (not a number) buy_amount
    ex =
      assertThrows {
        sep38Service.getPrice("iso4217:USD", null, null, stellarUSDC, "bar", null, null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("buy_amount is invalid", ex.message)

    // buy_amount should be positive
    ex =
      assertThrows {
        sep38Service.getPrice("iso4217:USD", null, null, stellarUSDC, "-0.02", null, null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("buy_amount should be positive", ex.message)

    // buy_amount should be positive
    ex =
      assertThrows {
        sep38Service.getPrice("iso4217:USD", null, null, stellarUSDC, "0", null, null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("buy_amount should be positive", ex.message)

    // unsupported sell_delivery_method
    ex =
      assertThrows {
        sep38Service.getPrice("iso4217:USD", "1.23", "FOO", stellarUSDC, null, null, null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported sell delivery method", ex.message)

    // unsupported buy_delivery_method
    ex =
      assertThrows {
        sep38Service.getPrice("iso4217:USD", "1.23", "WIRE", stellarUSDC, null, "BAR", null)
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported buy delivery method", ex.message)

    // unsupported country_code
    ex =
      assertThrows {
        sep38Service.getPrice("iso4217:USD", "1.23", "WIRE", stellarUSDC, null, null, "BRA")
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported country code", ex.message)
  }

  @Test
  fun test_getPrice_minimumParametersWithSellAmount() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(GetRateRequest.Type.INDICATIVE)
        .sellAsset("iso4217:USD")
        .sellAmount("100")
        .buyAsset(stellarUSDC)
        .build()
    every { mockRateIntegration.getRate(getRateReq) } returns GetRateResponse("1.02")
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, mockRateIntegration, null)

    // test happy path with the minimum parameters using sellAmount
    var gotResponse: GetPriceResponse? = null
    assertDoesNotThrow {
      gotResponse = sep38Service.getPrice("iso4217:USD", "100", null, stellarUSDC, null, null, null)
    }
    val wantResponse =
      GetPriceResponse.builder().price("1.02").sellAmount("100").buyAmount("98.0392157").build()
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun test_getPrice_minimumParametersWithBuyAmount() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(GetRateRequest.Type.INDICATIVE)
        .sellAsset("iso4217:USD")
        .buyAmount("100")
        .buyAsset(stellarUSDC)
        .build()
    every { mockRateIntegration.getRate(getRateReq) } returns GetRateResponse("1.02")
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, mockRateIntegration, null)

    // test happy path with the minimum parameters using buyAmount
    var gotResponse: GetPriceResponse? = null
    assertDoesNotThrow {
      gotResponse = sep38Service.getPrice("iso4217:USD", null, null, stellarUSDC, "100", null, null)
    }
    val wantResponse =
      GetPriceResponse.builder().price("1.02").sellAmount("102").buyAmount("100").build()
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun test_getPrice_allParametersWithSellAmount() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(GetRateRequest.Type.INDICATIVE)
        .sellAsset("iso4217:USD")
        .buyAsset(stellarUSDC)
        .sellAmount("100")
        .countryCode("USA")
        .sellDeliveryMethod("WIRE")
        .build()
    every { mockRateIntegration.getRate(getRateReq) } returns GetRateResponse("1.02")
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, mockRateIntegration, null)

    // test happy path with all the parameters using sellAmount
    var gotResponse: GetPriceResponse? = null

    assertDoesNotThrow {
      gotResponse =
        sep38Service.getPrice("iso4217:USD", "100", "WIRE", stellarUSDC, null, null, "USA")
    }
    val wantResponse =
      GetPriceResponse.builder().price("1.02").sellAmount("100").buyAmount("98.0392157").build()
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun test_getPrice_allParametersWithBuyAmount() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(GetRateRequest.Type.INDICATIVE)
        .sellAsset("iso4217:USD")
        .buyAsset(stellarUSDC)
        .buyAmount("100")
        .countryCode("USA")
        .sellDeliveryMethod("WIRE")
        .build()
    every { mockRateIntegration.getRate(getRateReq) } returns GetRateResponse("1.02345678901")
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, mockRateIntegration, null)

    // test happy path with all the parameters using buyAmount
    var gotResponse: GetPriceResponse? = null

    assertDoesNotThrow {
      gotResponse =
        sep38Service.getPrice("iso4217:USD", null, "WIRE", stellarUSDC, "100", null, "USA")
    }
    val wantResponse =
      GetPriceResponse.builder()
        .price("1.02345678901")
        .sellAmount("102.3457")
        .buyAmount("100")
        .build()
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun test_postQuote_failure() {
    // empty rateIntegration should throw an error
    var ex: AnchorException = assertThrows {
      sep38Service.postQuote(null, Sep38PostQuoteRequest.builder().build())
    }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("internal server error", ex.message)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, mockRateIntegration, null)

    // empty sep38QuoteStore should throw an error
    ex = assertThrows { sep38Service.postQuote(null, Sep38PostQuoteRequest.builder().build()) }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("internal server error", ex.message)

    // mocked quote store
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        quoteStore
      )

    // empty token
    ex = assertThrows { sep38Service.postQuote(null, Sep38PostQuoteRequest.builder().build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("missing sep10 jwt token", ex.message)

    // malformed token
    var token = createJwtToken("")
    ex = assertThrows { sep38Service.postQuote(token, Sep38PostQuoteRequest.builder().build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sep10 token is malformed", ex.message)

    // empty sell_asset
    token = createJwtToken()
    ex = assertThrows { sep38Service.postQuote(token, Sep38PostQuoteRequest.builder().build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_asset cannot be empty", ex.message)

    // nonexistent sell_asset
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder().sellAssetName("foo:bar").build()
        )
      }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("sell_asset not found", ex.message)

    // empty buy_asset
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder().sellAssetName("iso4217:USD").build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("buy_asset cannot be empty", ex.message)

    // nonexistent buy_asset
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .buyAssetName("foo:bar")
            .build()
        )
      }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("buy_asset not found", ex.message)

    // both sell_amount & buy_amount are empty
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .buyAssetName(stellarUSDC)
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Please provide either sell_amount or buy_amount", ex.message)

    // both sell_amount & buy_amount are filled
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .sellAmount("100")
            .buyAssetName(stellarUSDC)
            .buyAmount("100")
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Please provide either sell_amount or buy_amount", ex.message)

    // invalid (not a number) sell_amount
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .sellAmount("foo")
            .buyAssetName(stellarUSDC)
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount is invalid", ex.message)

    // sell_amount should be positive
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .sellAmount("-0.01")
            .buyAssetName(stellarUSDC)
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount should be positive", ex.message)

    // sell_amount should be positive
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .sellAmount("0")
            .buyAssetName(stellarUSDC)
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount should be positive", ex.message)

    // invalid (not a number) buy_amount
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .buyAssetName(stellarUSDC)
            .buyAmount("bar")
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("buy_amount is invalid", ex.message)

    // buy_amount should be positive
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .buyAssetName(stellarUSDC)
            .buyAmount("-0.02")
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("buy_amount should be positive", ex.message)

    // buy_amount should be positive
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .buyAssetName(stellarUSDC)
            .buyAmount("0")
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("buy_amount should be positive", ex.message)

    // unsupported sell_delivery_method
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .sellAmount("1.23")
            .sellDeliveryMethod("FOO")
            .buyAssetName(stellarUSDC)
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported sell delivery method", ex.message)

    // unsupported buy_delivery_method
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .sellAmount("1.23")
            .sellDeliveryMethod("WIRE")
            .buyAssetName(stellarUSDC)
            .buyDeliveryMethod("BAR")
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported buy delivery method", ex.message)

    // unsupported country_code
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .sellAmount("1.23")
            .sellDeliveryMethod("WIRE")
            .buyAssetName(stellarUSDC)
            .countryCode("BRA")
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported country code", ex.message)

    // unsupported expire_after
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .sellAmount("1.23")
            .sellDeliveryMethod("WIRE")
            .buyAssetName(stellarUSDC)
            .countryCode("USA")
            .expireAfter("FOO BAR")
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("expire_after is invalid", ex.message)
  }

  @Test
  fun test_postQuote_minimumParametersWithSellAmount() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(GetRateRequest.Type.FIRM)
        .sellAsset("iso4217:USD")
        .sellAmount("100")
        .buyAsset(stellarUSDC)
        .account(PUBLIC_KEY)
        .build()
    val tomorrow = Instant.now().plus(1, ChronoUnit.DAYS)
    every { mockRateIntegration.getRate(getRateReq) } returns
      GetRateResponse("123", "1.02", tomorrow)
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        quoteStore
      )

    val slotQuote = slot<Sep38Quote>()
    every { quoteStore.save(capture(slotQuote)) } returns null

    // test happy path with the minimum parameters using sellAmount
    val token = createJwtToken()
    var gotResponse: Sep38QuoteResponse? = null
    assertDoesNotThrow {
      gotResponse =
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .sellAmount("100")
            .buyAssetName(stellarUSDC)
            .build()
        )
    }
    val wantResponse =
      Sep38QuoteResponse.builder()
        .id("123")
        .expiresAt(tomorrow)
        .price("1.02")
        .sellAsset("iso4217:USD")
        .sellAmount("100")
        .buyAsset(stellarUSDC)
        .buyAmount("98.0392157")
        .build()
    assertEquals(wantResponse, gotResponse)

    // verify the saved quote
    verify(exactly = 1) { quoteStore.save(any()) }
    val savedQuote = slotQuote.captured
    assertEquals("123", savedQuote.id)
    assertEquals(tomorrow, savedQuote.expiresAt)
    assertEquals("1.02", savedQuote.price)
    assertEquals("iso4217:USD", savedQuote.sellAsset)
    assertEquals("100", savedQuote.sellAmount)
    assertEquals(stellarUSDC, savedQuote.buyAsset)
    assertEquals("98.0392157", savedQuote.buyAmount)
    assertEquals(PUBLIC_KEY, savedQuote.creatorAccountId)
    assertNotNull(savedQuote.createdAt)
  }

  @Test
  fun test_postQuote_minimumParametersWithBuyAmount() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(GetRateRequest.Type.FIRM)
        .sellAsset("iso4217:USD")
        .buyAsset(stellarUSDC)
        .buyAmount("100")
        .account(PUBLIC_KEY)
        .build()
    val tomorrow = Instant.now().plus(1, ChronoUnit.DAYS)
    every { mockRateIntegration.getRate(getRateReq) } returns
      GetRateResponse("456", "1.02", tomorrow)
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        quoteStore
      )

    val slotQuote = slot<Sep38Quote>()
    every { quoteStore.save(capture(slotQuote)) } returns null

    // test happy path with the minimum parameters using sellAmount
    val token = createJwtToken()
    var gotResponse: Sep38QuoteResponse? = null
    assertDoesNotThrow {
      gotResponse =
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .buyAssetName(stellarUSDC)
            .buyAmount("100")
            .build()
        )
    }
    val wantResponse =
      Sep38QuoteResponse.builder()
        .id("456")
        .expiresAt(tomorrow)
        .price("1.02")
        .sellAsset("iso4217:USD")
        .sellAmount("102")
        .buyAsset(stellarUSDC)
        .buyAmount("100")
        .build()
    assertEquals(wantResponse, gotResponse)

    // verify the saved quote
    verify(exactly = 1) { quoteStore.save(any()) }
    val savedQuote = slotQuote.captured
    assertEquals("456", savedQuote.id)
    assertEquals(tomorrow, savedQuote.expiresAt)
    assertEquals("1.02", savedQuote.price)
    assertEquals("iso4217:USD", savedQuote.sellAsset)
    assertEquals("102", savedQuote.sellAmount)
    assertEquals(stellarUSDC, savedQuote.buyAsset)
    assertEquals("100", savedQuote.buyAmount)
    assertEquals(PUBLIC_KEY, savedQuote.creatorAccountId)
    assertNotNull(savedQuote.createdAt)
  }

  @Test
  fun test_postQuote_allParametersWithSellAmount() {
    val now = Instant.now()
    val tomorrow = now.plus(1, ChronoUnit.DAYS)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(GetRateRequest.Type.FIRM)
        .sellAsset("iso4217:USD")
        .sellAmount("100")
        .sellDeliveryMethod("WIRE")
        .buyAsset(stellarUSDC)
        .countryCode("USA")
        .account(PUBLIC_KEY)
        .expireAfter(now.toString())
        .build()
    every { mockRateIntegration.getRate(getRateReq) } returns
      GetRateResponse("123", "1.02", tomorrow)
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        quoteStore
      )

    val slotQuote = slot<Sep38Quote>()
    every { quoteStore.save(capture(slotQuote)) } returns null

    // test happy path with the minimum parameters using sellAmount
    val token = createJwtToken()
    var gotResponse: Sep38QuoteResponse? = null
    assertDoesNotThrow {
      gotResponse =
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .sellAmount("100")
            .sellDeliveryMethod("WIRE")
            .buyAssetName(stellarUSDC)
            .countryCode("USA")
            .expireAfter(now.toString())
            .build()
        )
    }
    val wantResponse =
      Sep38QuoteResponse.builder()
        .id("123")
        .expiresAt(tomorrow)
        .price("1.02")
        .sellAsset("iso4217:USD")
        .sellAmount("100")
        .buyAsset(stellarUSDC)
        .buyAmount("98.0392157")
        .build()
    assertEquals(wantResponse, gotResponse)

    // verify the saved quote
    verify(exactly = 1) { quoteStore.save(any()) }
    val savedQuote = slotQuote.captured
    assertEquals("123", savedQuote.id)
    assertEquals(tomorrow, savedQuote.expiresAt)
    assertEquals("1.02", savedQuote.price)
    assertEquals("iso4217:USD", savedQuote.sellAsset)
    assertEquals("100", savedQuote.sellAmount)
    assertEquals("WIRE", savedQuote.sellDeliveryMethod)
    assertEquals(stellarUSDC, savedQuote.buyAsset)
    assertEquals("98.0392157", savedQuote.buyAmount)
    assertEquals(PUBLIC_KEY, savedQuote.creatorAccountId)
    assertNotNull(savedQuote.createdAt)
  }

  @Test
  fun test_postQuote_allParametersWithBuyAmount() {
    val now = Instant.now()
    val tomorrow = now.plus(1, ChronoUnit.DAYS)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(GetRateRequest.Type.FIRM)
        .sellAsset("iso4217:USD")
        .sellDeliveryMethod("WIRE")
        .buyAsset(stellarUSDC)
        .buyAmount("100")
        .countryCode("USA")
        .account(PUBLIC_KEY)
        .expireAfter(now.toString())
        .build()
    every { mockRateIntegration.getRate(getRateReq) } returns
      GetRateResponse("456", "1.02", tomorrow)
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        quoteStore
      )

    val slotQuote = slot<Sep38Quote>()
    every { quoteStore.save(capture(slotQuote)) } returns null

    // test happy path with the minimum parameters using sellAmount
    val token = createJwtToken()
    var gotResponse: Sep38QuoteResponse? = null
    assertDoesNotThrow {
      gotResponse =
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName("iso4217:USD")
            .sellDeliveryMethod("WIRE")
            .buyAssetName(stellarUSDC)
            .buyAmount("100")
            .countryCode("USA")
            .expireAfter(now.toString())
            .build()
        )
    }
    val wantResponse =
      Sep38QuoteResponse.builder()
        .id("456")
        .expiresAt(tomorrow)
        .price("1.02")
        .sellAsset("iso4217:USD")
        .sellAmount("102")
        .buyAsset(stellarUSDC)
        .buyAmount("100")
        .build()
    assertEquals(wantResponse, gotResponse)

    // verify the saved quote
    verify(exactly = 1) { quoteStore.save(any()) }
    val savedQuote = slotQuote.captured
    assertEquals("456", savedQuote.id)
    assertEquals(tomorrow, savedQuote.expiresAt)
    assertEquals("1.02", savedQuote.price)
    assertEquals("iso4217:USD", savedQuote.sellAsset)
    assertEquals("102", savedQuote.sellAmount)
    assertEquals("WIRE", savedQuote.sellDeliveryMethod)
    assertEquals(stellarUSDC, savedQuote.buyAsset)
    assertEquals("100", savedQuote.buyAmount)
    assertEquals(PUBLIC_KEY, savedQuote.creatorAccountId)
    assertNotNull(savedQuote.createdAt)
  }

  @Test
  fun test_getQuote_failure() {
    // empty sep38QuoteStore should throw an error
    var ex: AnchorException = assertThrows { sep38Service.getQuote(null, null) }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("internal server error", ex.message)

    // mocked quote store
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, null, quoteStore)

    // empty token
    ex = assertThrows { sep38Service.getQuote(null, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("missing sep10 jwt token", ex.message)

    // malformed token
    var token = createJwtToken("")
    ex = assertThrows { sep38Service.getQuote(token, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sep10 token is malformed", ex.message)

    // empty quote id
    token = createJwtToken()
    ex = assertThrows { sep38Service.getQuote(token, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("quote id cannot be empty", ex.message)

    // mock quote builder
    val now = Instant.now()
    val tomorrow = now.plus(1, ChronoUnit.DAYS)
    val mockQuoteBuilder: () -> Sep38QuoteBuilder = {
      Sep38QuoteBuilder(quoteStore)
        .id("123")
        .expiresAt(tomorrow)
        .price("1.02")
        .sellAsset("iso4217:USD")
        .sellAmount("100")
        .sellDeliveryMethod("WIRE")
        .buyAsset(stellarUSDC)
        .buyAmount("98.0392157")
        .createdAt(now)
    }

    // jwt token account is different from quote account
    val wrongAccount = "GB3MX4G2RSN5UC2GXLIQI7YAY3G5SH3TJZHT2WEDHGJLU5UW6IVXKGLL"
    var mockQuote = mockQuoteBuilder().creatorAccountId(wrongAccount).build()
    var slotQuoteId = slot<String>()
    every { quoteStore.findByQuoteId(capture(slotQuoteId)) } returns mockQuote
    ex = assertThrows { sep38Service.getQuote(token, "123") }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("quote not found", ex.message)
    verify(exactly = 1) { quoteStore.findByQuoteId(any()) }
    assertEquals("123", slotQuoteId.captured)

    // jwt token memo is different from quote memo
    mockQuote = mockQuoteBuilder().creatorAccountId(PUBLIC_KEY).creatorMemo("wrong memo!").build()
    slotQuoteId = slot()
    every { quoteStore.findByQuoteId(capture(slotQuoteId)) } returns mockQuote
    ex = assertThrows { sep38Service.getQuote(token, "123") }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("quote not found", ex.message)
    verify(exactly = 2) { quoteStore.findByQuoteId(any()) }
    assertEquals("123", slotQuoteId.captured)

    // jwt token memo is different from quote memo
    mockQuote =
      mockQuoteBuilder().creatorAccountId(PUBLIC_KEY).creatorMemoType("wrong memoType!").build()
    slotQuoteId = slot()
    every { quoteStore.findByQuoteId(capture(slotQuoteId)) } returns mockQuote
    ex = assertThrows { sep38Service.getQuote(token, "123") }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("quote not found", ex.message)
    verify(exactly = 3) { quoteStore.findByQuoteId(any()) }
    assertEquals("123", slotQuoteId.captured)
  }

  @Test
  fun test_getQuote() {
    // mocked quote store
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, null, quoteStore)

    // mock quote store response
    val now = Instant.now()
    val tomorrow = now.plus(1, ChronoUnit.DAYS)
    val mockQuote =
      Sep38QuoteBuilder(quoteStore)
        .id("123")
        .expiresAt(tomorrow)
        .price("1.02")
        .sellAsset("iso4217:USD")
        .sellAmount("100")
        .sellDeliveryMethod("WIRE")
        .buyAsset(stellarUSDC)
        .buyAmount("98.0392157")
        .createdAt(now)
        .creatorAccountId(PUBLIC_KEY)
        .build()
    val slotQuoteId = slot<String>()
    every { quoteStore.findByQuoteId(capture(slotQuoteId)) } returns mockQuote

    // execute request
    val token = createJwtToken()
    var gotQuoteResponse: Sep38QuoteResponse? = null
    assertDoesNotThrow { gotQuoteResponse = sep38Service.getQuote(token, "123") }

    // verify the store response was called as expected
    verify(exactly = 1) { quoteStore.findByQuoteId(any()) }
    assertEquals("123", slotQuoteId.captured)

    // verify results
    val wantQuoteResponse =
      Sep38QuoteResponse.builder()
        .id("123")
        .expiresAt(tomorrow)
        .price("1.02")
        .sellAsset("iso4217:USD")
        .sellAmount("100")
        .buyAsset(stellarUSDC)
        .buyAmount("98.0392157")
        .build()
    assertEquals(wantQuoteResponse, gotQuoteResponse)
  }

  private fun createJwtToken(publicKey: String = PUBLIC_KEY): JwtToken {
    val issuedAt: Long = System.currentTimeMillis() / 1000L
    return JwtToken.of(
      appConfig.hostUrl + "/auth",
      publicKey,
      issuedAt,
      issuedAt + 60,
      "",
      "vibrant.stellar.org"
    )
  }
}
