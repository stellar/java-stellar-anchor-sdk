package org.stellar.anchor.platform

import org.junit.jupiter.api.Assertions.*
import org.stellar.anchor.dto.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.dto.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Sep1Helper

const val postTxnJson =
  """{
    "amount": "10.0",
    "asset_code": "USDC",
    "asset_issuer": "GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN",
    "receiver_id": "MOCK_RECEIVER_ID",
    "sender_id": "MOCK_SENDER_ID",
    "fields": {
        "transaction": {
            "receiver_routing_number": "r0123",
            "receiver_account_number": "a0456",
            "type": "SWIFT"
        }
    }
}"""

fun sep31TestAll(toml: Sep1Helper.TomlContent, jwt: String) {
  println("Performing SEP31 tests...")
  sep12Client = Sep12Client(toml.getString("KYC_SERVER"), jwt)
  sep31Client = Sep31Client(toml.getString("DIRECT_PAYMENT_SERVER"), jwt)

  sep31TestInfo()
  sep31PostTransaction()
}

fun sep31TestInfo() {
  printRequest("Calling GET /info")
  val info = sep31Client.getInfo()
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

fun sep31PostTransaction() {
  // Create customer
  val customer =
    GsonUtils.getInstance().fromJson(testCustomerJson, Sep12PutCustomerRequest::class.java)
  val pr = sep12Client.putCustomer(customer)

  // Post Sep31 transaction.
  val txnRequest = gson.fromJson(postTxnJson, Sep31PostTransactionRequest::class.java)
  txnRequest.receiverId = pr!!.id
  val txn = sep31Client.postTransaction(txnRequest)

  println(txn)
  // TODO: Delete customer
}
