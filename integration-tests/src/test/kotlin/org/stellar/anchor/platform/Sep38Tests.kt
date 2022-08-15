package org.stellar.anchor.platform

import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.sep.sep38.GetPriceResponse
import org.stellar.anchor.api.sep.sep38.Sep38Context.*
import org.stellar.anchor.util.Sep1Helper

lateinit var sep38: Sep38Client

fun sep38TestAll(toml: Sep1Helper.TomlContent, jwt: String) {
  println("Performing SEP38 tests...")
  sep38 = Sep38Client(toml.getString("ANCHOR_QUOTE_SERVER"), jwt)

  sep38TestHappyPath()
  testSellOverAssetLimit()
}

fun sep38TestHappyPath() {
  // GET {SEP38}/info
  printRequest("Calling GET /info")
  val info = sep38.getInfo()
  printResponse(info)

  // GET {SEP38}/prices
  printRequest("Calling GET /prices")
  val prices = sep38.getPrices("iso4217:USD", "100")
  printResponse(prices)

  // GET {SEP38}/price
  printRequest("Calling GET /price")
  val price =
    sep38.getPrice(
      "iso4217:USD",
      "100",
      "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
      SEP31
    )
  printResponse(price)

  // POST {SEP38}/quote
  printRequest("Calling POST /quote")
  var postQuote =
    sep38.postQuote(
      "iso4217:USD",
      "100",
      "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
      SEP31
    )
  printResponse(postQuote)

  // POST {SEP38}/quote with `expires_after`
  printRequest("Calling POST /quote")
  val expireAfter = DateTimeFormatter.ISO_INSTANT.parse("2022-04-30T02:15:44.000Z", Instant::from)
  postQuote =
    sep38.postQuote(
      "iso4217:USD",
      "100",
      "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
      SEP31,
      expireAfter = expireAfter
    )
  printResponse(postQuote)

  // GET {SEP38}/quote/{id}
  printRequest("Calling GET /quote")
  val getQuote = sep38.getQuote(postQuote.id)
  printResponse(getQuote)
  assertEquals(postQuote, getQuote)
}

fun testSellOverAssetLimit() {
  printRequest("Calling GET /price")
  var price: GetPriceResponse?

  assertThrows<SepException> {
    price =
      sep38.getPrice(
        "iso4217:USD",
        "10000000000",
        "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
        SEP31
      )
  }
}
