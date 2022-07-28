@file:Suppress("UNCHECKED_CAST")

package org.stellar.anchor.platform

import org.junit.jupiter.api.Assertions.*
import org.stellar.anchor.util.Sep1Helper

lateinit var sep24Client: Sep24Client

const val withdrawJson =
  """{
    "asset_code": "USDC",
    "asset_issuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
    "account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
    "lang": "en"
}"""

fun sep24TestAll(toml: Sep1Helper.TomlContent, jwt: String) {
  println("Performing SEP24 tests...")
  sep24Client = Sep24Client(toml.getString("TRANSFER_SERVER_SEP0024"), jwt)

  testSep24TestInfo()
  testSep24PostInteractive()
}

fun testSep24TestInfo() {
  printRequest("Calling GET /info")
  val info = sep24Client.getInfo()
  printResponse(info)
  assertTrue(info.deposit.isNotEmpty())
  assertTrue(info.withdraw.isNotEmpty())
}

fun testSep24PostInteractive() {
  printRequest("POST /transactions/withdraw/interactive")
  val withdrawRequest = gson.fromJson(withdrawJson, HashMap::class.java)
  val txn = sep24Client.withdraw(withdrawRequest as HashMap<String, String>)
  printResponse("POST /transactions/withdraw/interactive response:", txn)
  val savedTxn = sep24Client.getTransaction(txn.id, "USDC")
  printResponse(savedTxn)
  assertEquals(txn.id, savedTxn.transaction.id)
  assertEquals("withdrawal", savedTxn.transaction.kind)
  assertEquals("incomplete", savedTxn.transaction.status)
  assertFalse(savedTxn.transaction.refunded)
  assertEquals(
    "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
    savedTxn.transaction.from
  )
}
