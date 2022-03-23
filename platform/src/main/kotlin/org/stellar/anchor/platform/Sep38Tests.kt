package org.stellar.anchor.platform

import org.stellar.anchor.util.Sep1Helper

lateinit var sep38: Sep38Client
lateinit var jwtStr: String

fun sep38TestAll(toml: Sep1Helper.TomlContent, jwt: String) {
  jwtStr = jwt
  println("Performing SEP38 tests...")
  sep38 = Sep38Client(toml.getString("ANCHOR_QUOTE_SERVER"))

  sep38TestHappyPath()
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

  // GET {SEP38}/prices
  printRequest("Calling GET /price")
  val price =
    sep38.getPrice(
      "iso4217:USD",
      "100",
      "stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN"
    )
  printResponse(price)
}
