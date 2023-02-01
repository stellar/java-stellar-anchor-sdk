package org.stellar.anchor.platform

import com.google.gson.Gson
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.api.callback.GetRateRequest
import org.stellar.anchor.api.exception.NotFoundException
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerRequest
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.api.sep.sep38.Sep38Context
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.platform.callback.RestCustomerIntegration
import org.stellar.anchor.platform.callback.RestFeeIntegration
import org.stellar.anchor.platform.callback.RestRateIntegration
import org.stellar.anchor.util.GsonUtils

class CallbackApiTests {
  companion object {
    private const val PLATFORM_TO_ANCHOR_SECRET = "myPlatformToAnchorSecret"
    private const val JWT_EXPIRATION_MILLISECONDS: Long = 10000
    private const val FIAT_USD = "iso4217:USD"
    private const val STELLAR_USD =
      "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"

    private val httpClient: OkHttpClient =
      OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .build()

    private val platformToAnchorJwtService = JwtService(PLATFORM_TO_ANCHOR_SECRET, null, null)

    private val authHelper =
      AuthHelper.forJwtToken(
        platformToAnchorJwtService,
        JWT_EXPIRATION_MILLISECONDS,
        "http://localhost:${AnchorPlatformIntegrationTest.SEP_SERVER_PORT}"
      )

    private val gson: Gson = GsonUtils.getInstance()

    private val rci =
      RestCustomerIntegration(
        "http://localhost:${AnchorPlatformIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        authHelper,
        gson
      )
    private val rriClient =
      RestRateIntegration(
        "http://localhost:${AnchorPlatformIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        authHelper,
        gson
      )
    private val rfiClient =
      RestFeeIntegration(
        "http://localhost:${AnchorPlatformIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        authHelper,
        gson
      )

    fun setup() {}

    @Test
    @Order(21)
    fun testCustomerIntegration() {
      assertThrows<NotFoundException> {
        rci.getCustomer(Sep12GetCustomerRequest.builder().id("1").build())
      }
    }

    fun testRate_indicativePrices() {
      val result =
        rriClient.getRate(
          GetRateRequest.builder()
            .type(GetRateRequest.Type.INDICATIVE_PRICES)
            .sellAsset(FIAT_USD)
            .sellAmount("100")
            .buyAsset(STELLAR_USD)
            .build()
        )
      Assertions.assertNotNull(result)
      val wantBody =
        """{
      "rate":{
        "price":"1.02",
        "sell_amount": "100",
        "buy_amount": "98.0392"
      }
    }"""
          .trimMargin()
      JSONAssert.assertEquals(wantBody, org.stellar.anchor.platform.gson.toJson(result), true)
    }

    @Test
    @Order(23)
    fun testRate_indicativePrice() {
      val result =
        rriClient.getRate(
          GetRateRequest.builder()
            .type(GetRateRequest.Type.INDICATIVE_PRICE)
            .context(Sep38Context.SEP31)
            .sellAsset(FIAT_USD)
            .sellAmount("100")
            .buyAsset(STELLAR_USD)
            .build()
        )
      Assertions.assertNotNull(result)
      val wantBody =
        """{
      "rate":{
        "total_price":"1.0303032801",
        "price":"1.0200002473",
        "sell_amount": "100",
        "buy_amount": "97.0588",
        "fee": {
          "total": "1.00",
          "asset": "$FIAT_USD",
          "details": [
            {
              "name": "Sell fee",
              "description": "Fee related to selling the asset.",
              "amount": "1.00"
            }
          ]
        }
      }
    }"""
          .trimMargin()
      JSONAssert.assertEquals(wantBody, org.stellar.anchor.platform.gson.toJson(result), true)
    }

    @Test
    @Order(24)
    fun testRate_firm() {
      val rate =
        rriClient
          .getRate(
            GetRateRequest.builder()
              .type(GetRateRequest.Type.FIRM)
              .context(Sep38Context.SEP31)
              .sellAsset(FIAT_USD)
              .buyAsset(STELLAR_USD)
              .buyAmount("100")
              .build()
          )
          .rate
      Assertions.assertNotNull(rate)

      // check if id is a valid UUID
      val id = rate.id
      Assertions.assertDoesNotThrow { UUID.fromString(id) }
      var gotExpiresAt: Instant? = null
      val expiresAtStr = rate.expiresAt.toString()
      Assertions.assertDoesNotThrow {
        gotExpiresAt = DateTimeFormatter.ISO_INSTANT.parse(rate.expiresAt.toString(), Instant::from)
      }

      val wantExpiresAt =
        ZonedDateTime.now(ZoneId.of("UTC"))
          .plusDays(1)
          .withHour(12)
          .withMinute(0)
          .withSecond(0)
          .withNano(0)
      assertEquals(wantExpiresAt.toInstant(), gotExpiresAt)

      // check if rate was persisted by getting the rate with ID
      val gotQuote = rriClient.getRate(GetRateRequest.builder().id(rate.id).build())
      assertEquals(rate.id, gotQuote.rate.id)
      assertEquals("1.02", gotQuote.rate.price)

      val wantBody =
        """{
      "rate":{
        "id": "$id",
        "total_price":"1.03",
        "price":"1.02",
        "sell_amount": "103",
        "buy_amount": "100",
        "expires_at": "$expiresAtStr",
        "fee": {
          "total": "1.00",
          "asset": "$FIAT_USD",
          "details": [
            {
              "name": "Sell fee",
              "description": "Fee related to selling the asset.",
              "amount": "1.00"
            }
          ]
        }
      }
    }"""
          .trimMargin()
      JSONAssert.assertEquals(wantBody, org.stellar.anchor.platform.gson.toJson(gotQuote), true)
    }

    @Test
    @Order(25)
    fun testGetFee() {
      // Create sender customer
      val senderCustomerRequest =
        GsonUtils.getInstance().fromJson(testCustomer1Json, Sep12PutCustomerRequest::class.java)
      val senderCustomer = sep12Client.putCustomer(senderCustomerRequest)

      // Create receiver customer
      val receiverCustomerRequest =
        GsonUtils.getInstance().fromJson(testCustomer2Json, Sep12PutCustomerRequest::class.java)
      val receiverCustomer = sep12Client.putCustomer(receiverCustomerRequest)

      val result =
        rfiClient.getFee(
          GetFeeRequest.builder()
            .sendAmount("10")
            .sendAsset("USDC")
            .receiveAsset("USDC")
            .senderId(senderCustomer!!.id)
            .receiverId(receiverCustomer!!.id)
            .clientId("<client-id>")
            .build()
        )

      Assertions.assertNotNull(result)
      JSONAssert.assertEquals(
        org.stellar.anchor.platform.gson.toJson(result),
        """{
        "fee": {
          "asset": "USDC",
          "amount": "0.30"
        }
      }""",
        true
      )
    }
  }
}

fun callbackApiTestAll() {
  CallbackApiTests.setup()

  println("Performing Callback API tests...")
  CallbackApiTests.setup()
}
