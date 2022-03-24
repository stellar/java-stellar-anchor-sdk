package org.stellar.anchor.platform

import org.junit.jupiter.api.Assertions.*
import org.stellar.anchor.util.Sep1Helper

lateinit var sep31: Sep31Client

fun sep31TestAll(toml: Sep1Helper.TomlContent, jwt: String) {
  println("Performing SEP31 tests...")

  sep31 = Sep31Client(toml.getString("DIRECT_PAYMENT_SERVER"), jwt)

  sep31TestInfo()
}

fun sep31TestInfo() {
  // GET {SEP31}/info
  printRequest("Calling GET /info")
  val info = sep31.getInfo()
  printResponse(info)
  assertEquals(1, info.receive.size)
  assertNotNull(info.receive.get("USDC"))
  assertTrue(info.receive.get("USDC")!!.quotesRequired)
  assertTrue(info.receive.get("USDC")!!.quotesRequired)
  assertNotNull(info.receive.get("USDC")!!.sep12)
  assertNotNull(info.receive.get("USDC")!!.sep12.sender)
  assertNotNull(info.receive.get("USDC")!!.sep12.receiver)
  assertNotNull(info.receive.get("USDC")!!.fields.transaction)
  assertEquals(3, info.receive.get("USDC")!!.fields.transaction.size)
}
