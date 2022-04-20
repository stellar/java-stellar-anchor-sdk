package org.stellar.anchor.server

import com.google.gson.Gson
import com.google.gson.JsonParser
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory
import org.stellar.anchor.reference.AnchorReferenceServer
import org.stellar.anchor.reference.model.Quote
import org.stellar.anchor.reference.repo.QuoteRepo
import org.stellar.anchor.util.GsonUtils
import org.stellar.platform.apis.callbacks.requests.GetCustomerRequest
import org.stellar.platform.apis.callbacks.requests.GetFeeRequest
import org.stellar.platform.apis.callbacks.responses.GetCustomerResponse
import org.stellar.platform.apis.callbacks.responses.GetFeeResponse

class AnchorReferenceServerIntegrationTest {
  companion object {
    private const val REFERENCE_SERVER_PORT = 8081
    const val fiatUSD = "iso4217:USD"
    const val stellarUSDC = "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    val gson: Gson = GsonUtils.builder().setPrettyPrinting().create()
    val restTemplate: RestTemplate = RestTemplate()

//    init {
//      restTemplate.uriTemplateHandler =
//        DefaultUriBuilderFactory("http://localhost:$REFERENCE_SERVER_PORT")
//    }

    @BeforeAll
    @JvmStatic
    fun setup() {
      AnchorReferenceServer.start(REFERENCE_SERVER_PORT, "/")
    }
  }

  @Autowired lateinit var quoteRepo: QuoteRepo

  @Test
  fun getFee() {
    val result =
      restGetFee(
        GetFeeRequest.builder()
          .sendAmount("10")
          .sendAsset("USDC")
          .receiveAsset("USDC")
          .senderId("sender_id")
          .receiverId("receiver_id")
          .build()
      )
    assertNotNull(result.body)
    JSONAssert.assertEquals(
      gson.toJson(result.body),
      """
         {
             "fee": {
                "asset": "USDC",
                "amount": "0.30"
             }
         }
      """,
      true
    )
  }

  private fun restGetFee(getFeeRequest: GetFeeRequest): ResponseEntity<GetFeeResponse> {
    val json = gson.toJson(getFeeRequest)
    val params = gson.fromJson(json, HashMap::class.java)
    val query =
      params
        .entries
        .stream()
        .map { p -> urlEncodeUTF8(p.key.toString()) + "=" + urlEncodeUTF8(p.value.toString()) }
        .reduce { p1, p2 -> "$p1&$p2" }
        .orElse("")
    return restTemplate.getForEntity("/fee?$query", GetFeeResponse::class.java, params)
  }

  private fun urlEncodeUTF8(s: String): String {
    return URLEncoder.encode(s, "UTF-8")
  }

  @Test
  fun getCustomer() {
    val result = restTemplate.getForEntity("http://localhost:8081/customer?id={id}", String::class.java, "1")
//    val result = restTemplate.getForEntity("/customer?id=1", GetCustomerResponse::class.java, "1")
    assertNotNull(result)
    assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
  }

  @Test
  fun getRate_indicative() {
    val result =
      restTemplate.getForEntity(
        "/rate?type={type}&sell_asset={sell_asset}&sell_amount={sell_amount}&buy_asset={buy_asset}",
        String::class.java,
        "indicative",
        fiatUSD,
        "100",
        stellarUSDC
      )
    assertNotNull(result)
    assertEquals(HttpStatus.OK, result.statusCode)

    val wantBody = """{"rate":{"price":"1.02"}}"""
    JSONAssert.assertEquals(wantBody, result.body, true)
  }

  @Test
  fun getRate_firm() {
    val result =
      restTemplate.getForEntity(
        "/rate?type={type}&sell_asset={sell_asset}&buy_asset={buy_asset}&buy_amount={buy_amount}",
        String::class.java,
        "firm",
        fiatUSD,
        stellarUSDC,
        "100"
      )
    assertNotNull(result)
    assertEquals(HttpStatus.OK, result.statusCode)

    val json = JsonParser.parseString(result.body).asJsonObject
    // check if id is a valid UUID
    val id: String = json.get("rate").asJsonObject.get("id").asString
    assertDoesNotThrow { UUID.fromString(id) }
    // check if expires_at is a valid date
    val expiresAtStr: String = json.get("rate").asJsonObject.get("expires_at").asString
    var gotExpiresAt: Instant? = null
    assertDoesNotThrow {
      gotExpiresAt = DateTimeFormatter.ISO_INSTANT.parse(expiresAtStr, Instant::from)
    }
    val wantExpiresAt =
      ZonedDateTime.now(ZoneId.of("UTC"))
        .plusDays(1)
        .withHour(12)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
    assertEquals(wantExpiresAt.toInstant(), gotExpiresAt)

    // check if rate was persisted
    val wantQuote = Quote()
    wantQuote.id = id
    wantQuote.sellAsset = fiatUSD
    wantQuote.buyAsset = stellarUSDC
    wantQuote.buyAmount = "100"
    wantQuote.price = "1.02"
    wantQuote.expiresAt = gotExpiresAt
    val quote = this.quoteRepo.findById(id).orElse(null)
    wantQuote.createdAt = quote.createdAt
    assertEquals(wantQuote, quote)
  }

  @Test
  fun getRate_byId() {
    // create firm rate
    var result =
      restTemplate.getForEntity(
        "/rate?type={type}&sell_asset={sell_asset}&buy_asset={buy_asset}&buy_amount={buy_amount}",
        String::class.java,
        "firm",
        fiatUSD,
        stellarUSDC,
        "100"
      )
    assertNotNull(result)
    assertEquals(HttpStatus.OK, result.statusCode)
    val createdRateJson = JsonParser.parseString(result.body).asJsonObject
    // check if id is a valid UUID
    val createdRateId: String = createdRateJson.get("rate").asJsonObject.get("id").asString
    assertDoesNotThrow { UUID.fromString(createdRateId) }

    // get the rate by id
    result =
      restTemplate.getForEntity(
        "/rate?id={id}",
        String::class.java,
        createdRateId,
        fiatUSD,
        stellarUSDC,
        "100"
      )
    assertNotNull(result)
    assertEquals(HttpStatus.OK, result.statusCode)
    val json = JsonParser.parseString(result.body).asJsonObject
    assertEquals(createdRateJson, json)

    // check if id is a valid UUID
    val id: String = json.get("rate").asJsonObject.get("id").asString
    assertDoesNotThrow { UUID.fromString(id) }
    // check if expires_at is a valid date
    val expiresAtStr: String = json.get("rate").asJsonObject.get("expires_at").asString
    var gotExpiresAt: Instant? = null
    assertDoesNotThrow {
      gotExpiresAt = DateTimeFormatter.ISO_INSTANT.parse(expiresAtStr, Instant::from)
    }
    val wantExpiresAt =
      ZonedDateTime.now(ZoneId.of("UTC"))
        .plusDays(1)
        .withHour(12)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
    assertEquals(wantExpiresAt.toInstant(), gotExpiresAt)

    // check if rate was persisted
    val wantQuote = Quote()
    wantQuote.id = id
    wantQuote.sellAsset = fiatUSD
    wantQuote.buyAsset = stellarUSDC
    wantQuote.buyAmount = "100"
    wantQuote.price = "1.02"
    wantQuote.expiresAt = gotExpiresAt
    val quote = this.quoteRepo.findById(id).orElse(null)
    wantQuote.createdAt = quote.createdAt
    assertEquals(wantQuote, quote)
  }
}
