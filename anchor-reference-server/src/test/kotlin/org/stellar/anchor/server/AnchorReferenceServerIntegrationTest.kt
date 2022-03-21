package org.stellar.anchor.server

import com.google.gson.Gson
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
import org.stellar.platform.apis.callbacks.requests.GetCustomerRequest
import org.stellar.platform.apis.callbacks.responses.GetCustomerResponse
import org.stellar.platform.apis.callbacks.responses.GetRateResponse

@SpringBootTest(
  classes = [AnchorReferenceServer::class],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(locations = ["classpath:/anchor-reference-server.yaml"])
class AnchorReferenceServerIntegrationTest {
  companion object {
    val gson = Gson()
  }

  @Autowired lateinit var restTemplate: TestRestTemplate
  @Autowired lateinit var quoteRepo: QuoteRepo

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
        stellarUSDC
      )
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
        GetRateResponse::class.java,
        "firm",
        fiatUSD,
        stellarUSDC,
        "100"
      )
    assertNotNull(result)
    assertEquals(HttpStatus.OK, result.statusCode)
    println(result.body)
    val rate = result.body!!.rate
    assertNotNull(rate)
    assertEquals("1.02", rate.price)
    assertNotNull(rate.expiresAt)

    // check if rate was persisted
    val wantQuote = Quote()
    wantQuote.id = rate.id
    wantQuote.sellAsset = fiatUSD
    wantQuote.buyAsset = stellarUSDC
    wantQuote.buyAmount = "100"
    wantQuote.price = "1.02"
    wantQuote.expiresAt = rate.expiresAt
    val quote = this.quoteRepo.findById(rate.id).orElse(null)
    wantQuote.createdAt = quote.createdAt
    assertEquals(wantQuote, quote)
  }
}
