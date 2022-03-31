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
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.TestPropertySource
import org.stellar.anchor.reference.AnchorReferenceServer
import org.stellar.anchor.reference.model.Quote
import org.stellar.anchor.reference.repo.QuoteRepo
import org.stellar.anchor.util.GsonUtils
import org.stellar.platform.apis.callbacks.requests.GetCustomerRequest
import org.stellar.platform.apis.callbacks.requests.GetFeeRequest
import org.stellar.platform.apis.callbacks.responses.GetCustomerResponse
import org.stellar.platform.apis.callbacks.responses.GetFeeResponse

@SpringBootTest(
    classes = [AnchorReferenceServer::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:/anchor-reference-server.yaml"])
class AnchorReferenceServerIntegrationTest {
  companion object {
    val gson: Gson = GsonUtils.builder().setPrettyPrinting().create()
  }

  @Autowired lateinit var restTemplate: TestRestTemplate
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
                .build())
    print(result.body)
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
        true)
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
    val result = restGetCustomer(GetCustomerRequest.builder().id("1").build())
    println(result.body)
    assertNotNull(result)
    assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
  }

  private fun restGetCustomer(
      getCustomerRequest: GetCustomerRequest
  ): ResponseEntity<GetCustomerResponse> {
    val json = gson.toJson(getCustomerRequest)
    val params = gson.fromJson(json, HashMap::class.java)

    return restTemplate.getForEntity("/customer?id={id}", GetCustomerResponse::class.java, params)
  }

  @Test
  fun getRate_indicative() {
    val fiatUSD = "iso4217:USD"
    val stellarUSDC = "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"

    val result =
        restTemplate.getForEntity(
            "/rate?type={type}&sell_asset={sell_asset}&sell_amount={sell_amount}&buy_asset={buy_asset}",
            String::class.java,
            "indicative",
            fiatUSD,
            "100",
            stellarUSDC)
    assertNotNull(result)
    assertEquals(HttpStatus.OK, result.statusCode)

    val wantBody = """{"rate":{"price":"1.02"}}"""
    JSONAssert.assertEquals(wantBody, result.body, true)
  }

  @Test
  fun getRate_firm() {
    val fiatUSD = "iso4217:USD"
    val stellarUSDC = "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"

    val result =
        restTemplate.getForEntity(
            "/rate?type={type}&sell_asset={sell_asset}&buy_asset={buy_asset}&buy_amount={buy_amount}",
            String::class.java,
            "firm",
            fiatUSD,
            stellarUSDC,
            "100")
    assertNotNull(result)
    assertEquals(HttpStatus.OK, result.statusCode)
    println(result.body)
    //    val json = JsonPrimitive(result.body)
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
}
