package org.stellar.anchor.sep38

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.TestHelper.Companion.createJwtToken
import org.stellar.anchor.api.callback.GetRateRequest
import org.stellar.anchor.api.callback.GetRateRequest.Type.*
import org.stellar.anchor.api.callback.GetRateResponse
import org.stellar.anchor.api.exception.AnchorException
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.NotFoundException
import org.stellar.anchor.api.exception.ServerErrorException
import org.stellar.anchor.api.sep.AssetInfo
import org.stellar.anchor.api.sep.sep38.*
import org.stellar.anchor.api.sep.sep38.Sep38Context.*
import org.stellar.anchor.api.shared.StellarId
import org.stellar.anchor.asset.ResourceJsonAssetService
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.config.Sep38Config
import org.stellar.anchor.event.EventPublishService
import org.stellar.anchor.event.models.QuoteEvent

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
    private const val stellarUSDC =
      "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
    private const val fiatUSD = "iso4217:USD"
    private const val stellarJPYC =
      "stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
  }

  private lateinit var sep38Service: Sep38Service

  // store/db related:
  @MockK(relaxed = true) private lateinit var quoteStore: Sep38QuoteStore

  // events related
  @MockK(relaxed = true) private lateinit var eventService: EventPublishService

  // sep10 related:
  @MockK(relaxed = true) private lateinit var appConfig: AppConfig

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    val assetService = ResourceJsonAssetService("test_assets.json")
    val assets = assetService.listAllAssets()
    val sep8Config = PropertySep38Config()
    this.sep38Service = Sep38Service(sep8Config, assetService, null, null, eventService)
    assertEquals(3, assets.size)

    // sep10 related:
    every { appConfig.jwtSecretKey } returns "secret"

    // store/db related:
    every { quoteStore.newInstance() } returns PojoSep38Quote()
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun `test GET info`() {
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
      listOf(fiatUSD, "stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
    assertTrue(usdcAsset.exchangeableAssetNames.containsAll(wantAssets))
    assertTrue(wantAssets.containsAll(usdcAsset.exchangeableAssetNames))

    val stellarJPYC =
      assetMap["stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"]
    assertNotNull(stellarJPYC)
    assertNull(stellarJPYC!!.countryCodes)
    assertNull(stellarJPYC.sellDeliveryMethods)
    assertNull(stellarJPYC.buyDeliveryMethods)
    wantAssets = listOf(fiatUSD, stellarUSDC)
    println(stellarJPYC.exchangeableAssetNames)
    assertTrue(stellarJPYC.exchangeableAssetNames.containsAll(wantAssets))
    assertTrue(wantAssets.containsAll(stellarJPYC.exchangeableAssetNames))

    val fiatUSD = assetMap[fiatUSD]
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
  fun `Test GET prices failure`() {
    // empty rateIntegration should throw an error
    var ex: AnchorException = assertThrows { sep38Service.getPrices(null, null, null, null, null) }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("internal server error", ex.message)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        null,
        null
      )

    // empty sell_asset
    ex = assertThrows { sep38Service.getPrices(null, null, null, null, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_asset cannot be empty", ex.message)

    // nonexistent sell_asset
    ex = assertThrows { sep38Service.getPrices("foo:bar", null, null, null, null) }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("sell_asset not found", ex.message)

    // empty sell_amount
    ex = assertThrows { sep38Service.getPrices(fiatUSD, null, null, null, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount cannot be empty", ex.message)

    // invalid (not a number) sell_amount
    ex = assertThrows { sep38Service.getPrices(fiatUSD, "foo", null, null, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount is invalid", ex.message)

    // sell_amount should be positive
    ex = assertThrows { sep38Service.getPrices(fiatUSD, "-0.01", null, null, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount should be positive", ex.message)

    // sell_amount should be positive
    ex = assertThrows { sep38Service.getPrices(fiatUSD, "0", null, null, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount should be positive", ex.message)

    // unsupported sell_delivery_method
    ex = assertThrows { sep38Service.getPrices(fiatUSD, "1.23", "FOO", null, null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported sell delivery method", ex.message)

    // unsupported country_code
    ex = assertThrows { sep38Service.getPrices(fiatUSD, "1.23", "WIRE", null, "FOO") }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported country code", ex.message)
  }

  @Test
  fun `test get prices with minimum parameters`() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq1 =
      GetRateRequest.builder()
        .type(INDICATIVE_PRICES)
        .sellAsset(fiatUSD)
        .buyAsset("stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
        .sellAmount("100")
        .build()
    every { mockRateIntegration.getRate(getRateReq1) } returns
      GetRateResponse.indicativePrices("1", "100", "100")

    val getRateReq2 =
      GetRateRequest.builder()
        .type(INDICATIVE_PRICES)
        .sellAsset(fiatUSD)
        .buyAsset(stellarUSDC)
        .sellAmount("100")
        .build()
    every { mockRateIntegration.getRate(getRateReq2) } returns
      GetRateResponse.indicativePrices("2", "100", "200")
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        null,
        null
      )

    // test happy path with the minimum parameters
    var gotResponse: GetPricesResponse? = null
    assertDoesNotThrow { gotResponse = sep38Service.getPrices(fiatUSD, "100", null, null, null) }
    val wantResponse = GetPricesResponse()
    wantResponse.addAsset(stellarJPYC, 7, "1")
    wantResponse.addAsset(stellarUSDC, 7, "2")
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun `test get prices with all parameters`() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq1 =
      GetRateRequest.builder()
        .type(INDICATIVE_PRICES)
        .sellAsset(fiatUSD)
        .buyAsset("stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
        .sellAmount("100")
        .countryCode("USA")
        .sellDeliveryMethod("WIRE")
        .build()
    every { mockRateIntegration.getRate(getRateReq1) } returns
      GetRateResponse.indicativePrices("1.1", "100", "110")

    val getRateReq2 =
      GetRateRequest.builder()
        .type(INDICATIVE_PRICES)
        .sellAsset(fiatUSD)
        .buyAsset(stellarUSDC)
        .sellAmount("100")
        .countryCode("USA")
        .sellDeliveryMethod("WIRE")
        .build()
    every { mockRateIntegration.getRate(getRateReq2) } returns
      GetRateResponse.indicativePrices("2.1", "100", "210")
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        null,
        null
      )

    // test happy path with all the parameters
    var gotResponse: GetPricesResponse? = null
    assertDoesNotThrow { gotResponse = sep38Service.getPrices(fiatUSD, "100", "WIRE", null, "USA") }
    val wantResponse = GetPricesResponse()
    wantResponse.addAsset(stellarJPYC, 7, "1.1")
    wantResponse.addAsset(stellarUSDC, 7, "2.1")
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun `Test get prices filter with buy delivery method`() {
    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq1 =
      GetRateRequest.builder()
        .type(INDICATIVE_PRICES)
        .sellAsset(stellarUSDC)
        .buyAsset(fiatUSD)
        .sellAmount("100")
        .buyDeliveryMethod("WIRE")
        .build()
    every { mockRateIntegration.getRate(getRateReq1) } returns
      GetRateResponse.indicativePrices("1", "100", "100")
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        null,
        null
      )

    // test happy path with the minimum parameters and specify buy_delivery_method
    var gotResponse: GetPricesResponse? = null
    assertDoesNotThrow {
      gotResponse = sep38Service.getPrices(stellarUSDC, "100", null, "WIRE", null)
    }
    val wantResponse = GetPricesResponse()
    wantResponse.addAsset(fiatUSD, 4, "1")
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun `test GET price failure`() {
    var getPriceRequestBuilder = Sep38GetPriceRequest.builder()

    // empty rateIntegration should throw an error
    var ex: AnchorException = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    var wantException: AnchorException = ServerErrorException("internal server error")
    assertEquals(wantException, ex)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        null,
        null
      )

    // empty sell_asset
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    wantException = BadRequestException("sell_asset cannot be empty")
    assertEquals(wantException, ex)

    // nonexistent sell_asset
    getPriceRequestBuilder = getPriceRequestBuilder.sellAssetName("foo:bar")
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("sell_asset not found", ex.message)

    // empty buy_asset
    getPriceRequestBuilder = getPriceRequestBuilder.sellAssetName(fiatUSD)
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    wantException = BadRequestException("buy_asset cannot be empty")
    assertEquals(wantException, ex)

    // nonexistent buy_asset
    getPriceRequestBuilder = getPriceRequestBuilder.buyAssetName("foo:bar")
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("buy_asset not found", ex.message)

    // both sell_amount & buy_amount are empty
    getPriceRequestBuilder = getPriceRequestBuilder.buyAssetName(stellarUSDC)
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Please provide either sell_amount or buy_amount", ex.message)

    // both sell_amount & buy_amount are filled
    getPriceRequestBuilder = getPriceRequestBuilder.sellAmount("100").buyAmount("100")
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Please provide either sell_amount or buy_amount", ex.message)

    // invalid (not a number) sell_amount
    getPriceRequestBuilder = getPriceRequestBuilder.sellAmount("foo").buyAmount(null)
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount is invalid", ex.message)

    // sell_amount should be positive
    getPriceRequestBuilder = getPriceRequestBuilder.sellAmount("-0.01")
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount should be positive", ex.message)

    // sell_amount should be positive
    getPriceRequestBuilder = getPriceRequestBuilder.sellAmount("0")
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount should be positive", ex.message)

    // invalid (not a number) buy_amount
    getPriceRequestBuilder = getPriceRequestBuilder.sellAmount(null).buyAmount("bar")
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("buy_amount is invalid", ex.message)

    // buy_amount should be positive
    getPriceRequestBuilder = getPriceRequestBuilder.buyAmount("-0.02")
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("buy_amount should be positive", ex.message)

    // buy_amount should be positive
    getPriceRequestBuilder = getPriceRequestBuilder.buyAmount("0")
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("buy_amount should be positive", ex.message)

    // unsupported sell_delivery_method
    getPriceRequestBuilder = getPriceRequestBuilder.sellAmount("1.23").buyAmount(null)
    getPriceRequestBuilder = getPriceRequestBuilder.sellDeliveryMethod("FOO")
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported sell delivery method", ex.message)

    // unsupported buy_delivery_method
    getPriceRequestBuilder = getPriceRequestBuilder.sellDeliveryMethod("WIRE")
    getPriceRequestBuilder = getPriceRequestBuilder.buyDeliveryMethod("BAR")
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported buy delivery method", ex.message)

    // unsupported country_code
    getPriceRequestBuilder = getPriceRequestBuilder.buyDeliveryMethod(null)
    getPriceRequestBuilder = getPriceRequestBuilder.countryCode("BRA")
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported country code", ex.message)

    // unsupported (null) context
    getPriceRequestBuilder = getPriceRequestBuilder.countryCode("USA")
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported context. Should be one of [sep6, sep31].", ex.message)

    // sell_amount should be within limit
    getPriceRequestBuilder = getPriceRequestBuilder.context(SEP31).sellAmount("100000000")
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount exceeds max limit", ex.message)

    // sell_amount should be positive
    getPriceRequestBuilder = getPriceRequestBuilder.context(SEP31).sellAmount("0.5")
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount less than min limit", ex.message)

    // buy_amount specified, but resulting sell_amount should be within limit
    getPriceRequestBuilder = getPriceRequestBuilder.sellAmount(null)
    getPriceRequestBuilder = getPriceRequestBuilder.buyAssetName(stellarUSDC).buyAmount("100000000")
    every { mockRateIntegration.getRate(any()) } returns
      GetRateResponse.indicativePrice(
        "1.02",
        "1.03",
        "102000000",
        "100000000",
        mockSellAssetFee(fiatUSD)
      )
    ex = assertThrows { sep38Service.getPrice(getPriceRequestBuilder.build()) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount exceeds max limit", ex.message)
  }

  private fun mockSellAssetFee(sellAsset: String?): RateFee {
    assertNotNull(sellAsset)

    val rateFee = RateFee("0", sellAsset)
    rateFee.addFeeDetail(RateFeeDetail("Sell fee", "1.00"))
    return rateFee
  }

  @Test
  fun `test GET price with minimum parameters and sell amount`() {
    val mockFee = mockSellAssetFee(fiatUSD)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(INDICATIVE_PRICE)
        .context(SEP31)
        .sellAsset(fiatUSD)
        .sellAmount("100")
        .buyAsset(stellarUSDC)
        .build()
    every { mockRateIntegration.getRate(getRateReq) } returns
      GetRateResponse.indicativePrice("1.02", "1.03", "100", "97.0874", mockFee)
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        null,
        null
      )

    // test happy path with the minimum parameters using sellAmount
    val getPriceRequest =
      Sep38GetPriceRequest.builder()
        .sellAssetName(fiatUSD)
        .sellAmount("100")
        .buyAssetName(stellarUSDC)
        .context(SEP31)
        .build()
    var gotResponse: GetPriceResponse? = null
    assertDoesNotThrow { gotResponse = sep38Service.getPrice(getPriceRequest) }
    val wantResponse =
      GetPriceResponse.builder()
        .price("1.02")
        .totalPrice("1.03")
        .sellAmount("100")
        .buyAmount("97.0874")
        .fee(mockFee)
        .build()
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun `test get price with minimum parameters and buy amount`() {
    val mockFee = mockSellAssetFee(fiatUSD)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(INDICATIVE_PRICE)
        .context(SEP31)
        .sellAsset(fiatUSD)
        .buyAmount("100")
        .buyAsset(stellarUSDC)
        .build()
    every { mockRateIntegration.getRate(getRateReq) } returns
      GetRateResponse.indicativePrice("1.02", "1.03", "103", "100", mockFee)
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        null,
        null
      )

    // test happy path with the minimum parameters using buyAmount
    val getPriceRequest =
      Sep38GetPriceRequest.builder()
        .sellAssetName(fiatUSD)
        .buyAssetName(stellarUSDC)
        .buyAmount("100")
        .context(SEP31)
        .build()
    var gotResponse: GetPriceResponse? = null
    assertDoesNotThrow { gotResponse = sep38Service.getPrice(getPriceRequest) }
    val wantResponse =
      GetPriceResponse.builder()
        .price("1.02")
        .totalPrice("1.03")
        .sellAmount("103")
        .buyAmount("100")
        .fee(mockFee)
        .build()
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun `test GET price all parameters with sell amount`() {
    val mockFee = mockSellAssetFee(fiatUSD)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(INDICATIVE_PRICE)
        .context(SEP6)
        .sellAsset(fiatUSD)
        .buyAsset(stellarUSDC)
        .sellAmount("100")
        .countryCode("USA")
        .sellDeliveryMethod("WIRE")
        .build()
    every { mockRateIntegration.getRate(getRateReq) } returns
      GetRateResponse.indicativePrice("1.02", "1.03", "100", "97.0873786", mockFee)
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        null,
        null
      )

    // test happy path with all the parameters using sellAmount
    val getPriceRequest =
      Sep38GetPriceRequest.builder()
        .sellAssetName(fiatUSD)
        .sellAmount("100")
        .sellDeliveryMethod("WIRE")
        .buyAssetName(stellarUSDC)
        .countryCode("USA")
        .context(SEP6)
        .build()
    var gotResponse: GetPriceResponse? = null
    assertDoesNotThrow { gotResponse = sep38Service.getPrice(getPriceRequest) }
    val wantResponse =
      GetPriceResponse.builder()
        .price("1.02")
        .totalPrice("1.03")
        .sellAmount("100")
        .buyAmount("97.0873786")
        .fee(mockFee)
        .build()
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun `test GET price all parameters with buy amount`() {
    val mockFee = mockSellAssetFee(fiatUSD)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(INDICATIVE_PRICE)
        .context(SEP31)
        .sellAsset(fiatUSD)
        .buyAsset(stellarUSDC)
        .buyAmount("100")
        .countryCode("USA")
        .sellDeliveryMethod("WIRE")
        .build()

    every { mockRateIntegration.getRate(getRateReq) } returns
      GetRateResponse.indicativePrice("1.02345678901", "1.03345679", "103.3456789", "100", mockFee)
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        null,
        null
      )

    // test happy path with all the parameters using buyAmount
    val getPriceRequest =
      Sep38GetPriceRequest.builder()
        .sellAssetName(fiatUSD)
        .sellDeliveryMethod("WIRE")
        .buyAssetName(stellarUSDC)
        .buyAmount("100")
        .countryCode("USA")
        .context(SEP31)
        .build()
    var gotResponse: GetPriceResponse? = null
    assertDoesNotThrow { gotResponse = sep38Service.getPrice(getPriceRequest) }
    val wantResponse =
      GetPriceResponse.builder()
        .price("1.02345678901")
        .totalPrice("1.03345679")
        .sellAmount("103.3456789")
        .buyAmount("100")
        .fee(mockFee)
        .build()
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun `test POST quote failures`() {
    // empty rateIntegration should throw an error
    var ex: AnchorException = assertThrows {
      sep38Service.postQuote(null, Sep38PostQuoteRequest.builder().build())
    }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("internal server error", ex.message)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        null,
        null
      )

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
        quoteStore,
        null
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
          Sep38PostQuoteRequest.builder().sellAssetName(fiatUSD).build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("buy_asset cannot be empty", ex.message)

    // nonexistent buy_asset
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder().sellAssetName(fiatUSD).buyAssetName("foo:bar").build()
        )
      }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("buy_asset not found", ex.message)

    // both sell_amount & buy_amount are empty
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder().sellAssetName(fiatUSD).buyAssetName(stellarUSDC).build()
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
            .sellAssetName(fiatUSD)
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
            .sellAssetName(fiatUSD)
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
            .sellAssetName(fiatUSD)
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
            .sellAssetName(fiatUSD)
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
            .sellAssetName(fiatUSD)
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
            .sellAssetName(fiatUSD)
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
            .sellAssetName(fiatUSD)
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
            .sellAssetName(fiatUSD)
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
            .sellAssetName(fiatUSD)
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
            .sellAssetName(fiatUSD)
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
            .sellAssetName(fiatUSD)
            .sellAmount("1.23")
            .sellDeliveryMethod("WIRE")
            .buyAssetName(stellarUSDC)
            .countryCode("USA")
            .expireAfter("2022-04-18T23:33:24.629719Z")
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("Unsupported context. Should be one of [sep6, sep31].", ex.message)

    // sell_amount should be within limit
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName(fiatUSD)
            .sellAmount("100000000")
            .sellDeliveryMethod("WIRE")
            .context(SEP31)
            .buyAssetName(stellarUSDC)
            .countryCode("USA")
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount exceeds max limit", ex.message)

    // sell_amount should be positive
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName(fiatUSD)
            .sellAmount("0.5")
            .sellDeliveryMethod("WIRE")
            .context(SEP31)
            .buyAssetName(stellarUSDC)
            .countryCode("USA")
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount less than min limit", ex.message)

    // buy_amount specified, but resulting sell_amount should be within limit
    every { mockRateIntegration.getRate(any()) } returns
      GetRateResponse.indicativePrice(
        "1.02",
        "1.03",
        "102000000",
        "100000000",
        mockSellAssetFee(fiatUSD)
      )
    ex =
      assertThrows {
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .sellAssetName(fiatUSD)
            .sellDeliveryMethod("WIRE")
            .context(SEP31)
            .buyAssetName(stellarUSDC)
            .buyAmount("100000000")
            .countryCode("USA")
            .build()
        )
      }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sell_amount exceeds max limit", ex.message)
  }

  @Test
  fun `test POST quote with minimum parameters and sell amount`() {
    val mockFee = mockSellAssetFee(fiatUSD)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(FIRM)
        .context(SEP31)
        .sellAsset(fiatUSD)
        .sellAmount("103")
        .buyAsset(stellarUSDC)
        .clientId(PUBLIC_KEY)
        .build()
    val tomorrow = Instant.now().plus(1, ChronoUnit.DAYS)

    val rate =
      GetRateResponse.Rate.builder()
        .id("123")
        .totalPrice("1.03")
        .price("1.02")
        .sellAmount("103")
        .buyAmount("100")
        .expiresAt(tomorrow)
        .fee(mockFee)
        .build()
    val wantRateResponse = GetRateResponse(rate)
    every { mockRateIntegration.getRate(getRateReq) } returns wantRateResponse
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        quoteStore,
        eventService
      )

    val slotQuote = slot<Sep38Quote>()
    every { quoteStore.save(capture(slotQuote)) } returns null

    // Mock event service
    val slotEvent = slot<QuoteEvent>()
    every { eventService.publish(capture(slotEvent)) } just Runs

    // test happy path with the minimum parameters using sellAmount
    val token = createJwtToken()
    var gotResponse: Sep38QuoteResponse? = null
    assertDoesNotThrow {
      gotResponse =
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .context(SEP31)
            .sellAssetName(fiatUSD)
            .sellAmount("103")
            .buyAssetName(stellarUSDC)
            .build()
        )
    }
    val wantResponse =
      Sep38QuoteResponse.builder()
        .id("123")
        .expiresAt(tomorrow)
        .price("1.02")
        .totalPrice("1.03")
        .sellAsset(fiatUSD)
        .sellAmount("103")
        .buyAsset(stellarUSDC)
        .buyAmount("100")
        .fee(mockFee)
        .build()
    assertEquals(wantResponse, gotResponse)

    // verify the saved quote
    verify(exactly = 1) { quoteStore.save(any()) }
    val savedQuote = slotQuote.captured
    assertEquals("123", savedQuote.id)
    assertEquals(tomorrow, savedQuote.expiresAt)
    assertEquals("1.02", savedQuote.price)
    assertEquals("1.03", savedQuote.totalPrice)
    assertEquals(fiatUSD, savedQuote.sellAsset)
    assertEquals("103", savedQuote.sellAmount)
    assertEquals(stellarUSDC, savedQuote.buyAsset)
    assertEquals("100", savedQuote.buyAmount)
    assertEquals(PUBLIC_KEY, savedQuote.creatorAccountId)
    assertNotNull(savedQuote.createdAt)
    assertEquals(mockFee, savedQuote.fee)

    // verify the published event
    verify(exactly = 1) { eventService.publish(any()) }
    val wantEvent =
      QuoteEvent.builder()
        .eventId(slotEvent.captured.eventId)
        .type(QuoteEvent.Type.QUOTE_CREATED)
        .id("123")
        .sellAsset(fiatUSD)
        .sellAmount("103")
        .buyAsset(stellarUSDC)
        .buyAmount("100")
        .expiresAt(tomorrow)
        .price("1.02")
        .totalPrice("1.03")
        .creator(StellarId.builder().account(PUBLIC_KEY).build())
        .transactionId(null)
        .createdAt(savedQuote.createdAt)
        .fee(mockFee)
        .build()
    assertEquals(wantEvent, slotEvent.captured)
  }

  @Test
  fun `test POST quote with minimum parameters and buy amount`() {
    val mockFee = mockSellAssetFee(fiatUSD)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(FIRM)
        .context(SEP6)
        .sellAsset(fiatUSD)
        .buyAsset(stellarUSDC)
        .buyAmount("100")
        .clientId(PUBLIC_KEY)
        .build()

    val tomorrow = Instant.now().plus(1, ChronoUnit.DAYS)
    val rate =
      GetRateResponse.Rate.builder()
        .id("456")
        .totalPrice("1.03")
        .price("1.02")
        .sellAmount("103")
        .buyAmount("100")
        .expiresAt(tomorrow)
        .fee(mockFee)
        .build()
    val wantRateResponse = GetRateResponse(rate)
    every { mockRateIntegration.getRate(getRateReq) } returns wantRateResponse
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        quoteStore,
        eventService,
      )

    val slotQuote = slot<Sep38Quote>()
    every { quoteStore.save(capture(slotQuote)) } returns null

    // Mock event service
    val slotEvent = slot<QuoteEvent>()
    every { eventService.publish(capture(slotEvent)) } just Runs

    // test happy path with the minimum parameters using sellAmount
    val token = createJwtToken()
    var gotResponse: Sep38QuoteResponse? = null
    assertDoesNotThrow {
      gotResponse =
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .context(SEP6)
            .sellAssetName(fiatUSD)
            .buyAssetName(stellarUSDC)
            .buyAmount("100")
            .build()
        )
    }
    val wantResponse =
      Sep38QuoteResponse.builder()
        .id("456")
        .expiresAt(tomorrow)
        .totalPrice("1.03")
        .price("1.02")
        .sellAsset(fiatUSD)
        .sellAmount("103")
        .buyAsset(stellarUSDC)
        .buyAmount("100")
        .fee(mockFee)
        .build()
    assertEquals(wantResponse, gotResponse)

    // verify the saved quote
    verify(exactly = 1) { quoteStore.save(any()) }
    val savedQuote = slotQuote.captured
    assertEquals("456", savedQuote.id)
    assertEquals(tomorrow, savedQuote.expiresAt)
    assertEquals("1.02", savedQuote.price)
    assertEquals("1.03", savedQuote.totalPrice)
    assertEquals(fiatUSD, savedQuote.sellAsset)
    assertEquals("103", savedQuote.sellAmount)
    assertEquals(stellarUSDC, savedQuote.buyAsset)
    assertEquals("100", savedQuote.buyAmount)
    assertEquals(PUBLIC_KEY, savedQuote.creatorAccountId)
    assertNotNull(savedQuote.createdAt)

    // verify the published event
    verify(exactly = 1) { eventService.publish(any()) }
    val wantEvent =
      QuoteEvent.builder()
        .eventId(slotEvent.captured.eventId)
        .type(QuoteEvent.Type.QUOTE_CREATED)
        .id("456")
        .sellAsset(fiatUSD)
        .sellAmount("103")
        .buyAsset(stellarUSDC)
        .buyAmount("100")
        .expiresAt(tomorrow)
        .price("1.02")
        .totalPrice("1.03")
        .creator(StellarId.builder().account(PUBLIC_KEY).build())
        .transactionId(null)
        .createdAt(savedQuote.createdAt)
        .fee(mockFee)
        .build()
    assertEquals(wantEvent, slotEvent.captured)
  }

  @Test
  fun `test POST quote with all parameters and sell amount`() {
    val mockFee = mockSellAssetFee(fiatUSD)
    val now = Instant.now()
    val tomorrow = now.plus(1, ChronoUnit.DAYS)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(FIRM)
        .context(SEP31)
        .sellAsset(fiatUSD)
        .sellAmount("100")
        .sellDeliveryMethod("WIRE")
        .buyAsset(stellarUSDC)
        .countryCode("USA")
        .clientId(PUBLIC_KEY)
        .expireAfter(now.toString())
        .build()

    val rate =
      GetRateResponse.Rate.builder()
        .id("123")
        .totalPrice("1.03")
        .price("1.02")
        .sellAmount("100")
        .buyAmount("97.0873786")
        .expiresAt(tomorrow)
        .fee(mockFee)
        .build()
    val wantRateResponse = GetRateResponse(rate)
    every { mockRateIntegration.getRate(getRateReq) } returns wantRateResponse
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        quoteStore,
        eventService
      )

    val slotQuote = slot<Sep38Quote>()
    every { quoteStore.save(capture(slotQuote)) } returns null

    // Mock event service
    val slotEvent = slot<QuoteEvent>()
    every { eventService.publish(capture(slotEvent)) } just Runs

    // test happy path with the minimum parameters using sellAmount
    val token = createJwtToken()
    var gotResponse: Sep38QuoteResponse? = null
    assertDoesNotThrow {
      gotResponse =
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .context(SEP31)
            .sellAssetName(fiatUSD)
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
        .totalPrice("1.03")
        .sellAsset(fiatUSD)
        .sellAmount("100")
        .buyAsset(stellarUSDC)
        .buyAmount("97.0873786")
        .fee(mockFee)
        .build()
    assertEquals(wantResponse, gotResponse)

    // verify the saved quote
    verify(exactly = 1) { quoteStore.save(any()) }
    val savedQuote = slotQuote.captured
    assertEquals("123", savedQuote.id)
    assertEquals(tomorrow, savedQuote.expiresAt)
    assertEquals("1.02", savedQuote.price)
    assertEquals("1.03", savedQuote.totalPrice)
    assertEquals(fiatUSD, savedQuote.sellAsset)
    assertEquals("100", savedQuote.sellAmount)
    assertEquals("WIRE", savedQuote.sellDeliveryMethod)
    assertEquals(stellarUSDC, savedQuote.buyAsset)
    assertEquals("97.0873786", savedQuote.buyAmount)
    assertEquals(PUBLIC_KEY, savedQuote.creatorAccountId)
    assertNotNull(savedQuote.createdAt)
    assertEquals(mockFee, savedQuote.fee)

    // verify the published event
    verify(exactly = 1) { eventService.publish(any()) }
    val wantEvent =
      QuoteEvent.builder()
        .eventId(slotEvent.captured.eventId)
        .type(QuoteEvent.Type.QUOTE_CREATED)
        .id("123")
        .sellAsset(fiatUSD)
        .sellAmount("100")
        .buyAsset(stellarUSDC)
        .buyAmount("97.0873786")
        .expiresAt(tomorrow)
        .price("1.02")
        .totalPrice("1.03")
        .creator(StellarId.builder().account(PUBLIC_KEY).build())
        .transactionId(null)
        .createdAt(savedQuote.createdAt)
        .fee(mockFee)
        .build()
    assertEquals(wantEvent, slotEvent.captured)
  }

  @Test
  fun `Test POST quote with all parameters and buy amount`() {
    val mockFee = mockSellAssetFee(fiatUSD)
    val now = Instant.now()
    val tomorrow = now.plus(1, ChronoUnit.DAYS)

    // mock rate integration
    val mockRateIntegration = mockk<MockRateIntegration>()
    val getRateReq =
      GetRateRequest.builder()
        .type(FIRM)
        .context(SEP31)
        .sellAsset(fiatUSD)
        .sellDeliveryMethod("WIRE")
        .buyAsset(stellarUSDC)
        .buyAmount("100")
        .countryCode("USA")
        .clientId(PUBLIC_KEY)
        .expireAfter(now.toString())
        .build()

    val rate =
      GetRateResponse.Rate.builder()
        .id("456")
        .totalPrice("1.03")
        .price("1.02")
        .sellAmount("103")
        .buyAmount("100")
        .expiresAt(tomorrow)
        .fee(mockFee)
        .build()
    val wantRateResponse = GetRateResponse(rate)
    every { mockRateIntegration.getRate(getRateReq) } returns wantRateResponse
    sep38Service =
      Sep38Service(
        sep38Service.sep38Config,
        sep38Service.assetService,
        mockRateIntegration,
        quoteStore,
        eventService
      )

    val slotQuote = slot<Sep38Quote>()
    every { quoteStore.save(capture(slotQuote)) } returns null

    // Mock event service
    val slotEvent = slot<QuoteEvent>()
    every { eventService.publish(capture(slotEvent)) } just Runs

    // test happy path with the minimum parameters using sellAmount
    val token = createJwtToken()
    var gotResponse: Sep38QuoteResponse? = null
    assertDoesNotThrow {
      gotResponse =
        sep38Service.postQuote(
          token,
          Sep38PostQuoteRequest.builder()
            .context(SEP31)
            .sellAssetName(fiatUSD)
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
        .totalPrice("1.03")
        .sellAsset(fiatUSD)
        .sellAmount("103")
        .buyAsset(stellarUSDC)
        .buyAmount("100")
        .fee(mockFee)
        .build()
    assertEquals(wantResponse, gotResponse)

    // verify the saved quote
    verify(exactly = 1) { quoteStore.save(any()) }
    val savedQuote = slotQuote.captured
    assertEquals("456", savedQuote.id)
    assertEquals(tomorrow, savedQuote.expiresAt)
    assertEquals("1.02", savedQuote.price)
    assertEquals("1.03", savedQuote.totalPrice)
    assertEquals(fiatUSD, savedQuote.sellAsset)
    assertEquals("103", savedQuote.sellAmount)
    assertEquals("WIRE", savedQuote.sellDeliveryMethod)
    assertEquals(stellarUSDC, savedQuote.buyAsset)
    assertEquals("100", savedQuote.buyAmount)
    assertEquals(PUBLIC_KEY, savedQuote.creatorAccountId)
    assertEquals(mockFee, savedQuote.fee)
    assertNotNull(savedQuote.createdAt)

    // verify the published event
    verify(exactly = 1) { eventService.publish(any()) }
    val wantEvent =
      QuoteEvent.builder()
        .eventId(slotEvent.captured.eventId)
        .type(QuoteEvent.Type.QUOTE_CREATED)
        .id("456")
        .sellAsset(fiatUSD)
        .sellAmount("103")
        .buyAsset(stellarUSDC)
        .buyAmount("100")
        .expiresAt(tomorrow)
        .price("1.02")
        .totalPrice("1.03")
        .creator(StellarId.builder().account(PUBLIC_KEY).build())
        .transactionId(null)
        .createdAt(savedQuote.createdAt)
        .fee(mockFee)
        .build()
    assertEquals(wantEvent, slotEvent.captured)
  }

  @Test
  fun `test GET quote failure`() {
    // empty sep38QuoteStore should throw an error
    var ex: AnchorException = assertThrows { sep38Service.getQuote(null, null) }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("internal server error", ex.message)

    // mocked quote store
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, null, quoteStore, null)

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
        .sellAsset(fiatUSD)
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

    // quote not found
    every { quoteStore.findByQuoteId(capture(slotQuoteId)) } returns null
    ex = assertThrows { sep38Service.getQuote(token, "444") }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("quote not found", ex.message)
    verify(exactly = 4) { quoteStore.findByQuoteId(any()) }
    assertEquals("444", slotQuoteId.captured)
  }

  @Test
  fun `Test GET quote`() {
    val mockFee = mockSellAssetFee(fiatUSD)

    // mocked quote store
    sep38Service =
      Sep38Service(sep38Service.sep38Config, sep38Service.assetService, null, quoteStore, null)

    // mock quote store response
    val now = Instant.now()
    val tomorrow = now.plus(1, ChronoUnit.DAYS)
    val mockQuote =
      Sep38QuoteBuilder(quoteStore)
        .id("123")
        .expiresAt(tomorrow)
        .price("1.02")
        .totalPrice("1.03")
        .sellAsset(fiatUSD)
        .sellAmount("100")
        .sellDeliveryMethod("WIRE")
        .buyAsset(stellarUSDC)
        .buyAmount("97.0873786")
        .createdAt(now)
        .creatorAccountId(PUBLIC_KEY)
        .fee(mockFee)
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
        .totalPrice("1.03")
        .sellAsset(fiatUSD)
        .sellAmount("100")
        .buyAsset(stellarUSDC)
        .buyAmount("97.0873786")
        .fee(mockFee)
        .build()
    assertEquals(wantQuoteResponse, gotQuoteResponse)
  }
}
