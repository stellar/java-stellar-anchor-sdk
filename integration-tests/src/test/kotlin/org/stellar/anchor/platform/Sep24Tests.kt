package org.stellar.anchor.platform

import org.junit.jupiter.api.Assertions.assertTrue
import org.stellar.anchor.util.Sep1Helper

lateinit var sep24Client: Sep24Client

fun sep24TestAll(toml: Sep1Helper.TomlContent, jwt: String) {
  println("Performing SEP24 tests...")
  sep24Client = Sep24Client(toml.getString("TRANSFER_SERVER_SEP0024"), jwt)

  testSep24TestInfo()
}

fun testSep24TestInfo() {
  printRequest("Calling GET /info")
  val info = sep24Client.getInfo()
  printResponse(info)
  assertTrue(info.deposit.isNotEmpty())
  assertTrue(info.withdraw.isNotEmpty())
}
