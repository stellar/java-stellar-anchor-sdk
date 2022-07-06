package org.stellar.anchor.platform

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Sep1Helper

lateinit var sep31Client: Sep31Client

const val postTxnJson =
  """{
    "amount": "10",
    "asset_code": "USDC",
    "asset_issuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
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
  sep38 = Sep38Client(toml.getString("ANCHOR_QUOTE_SERVER"), jwt)

  testSep31TestInfo()
  testSep31PostTransaction()
  testBadAsset()
}

fun testSep31TestInfo() {
  printRequest("Calling GET /info")
  val info = sep31Client.getInfo()
  printResponse(info)
  assertTrue(info.receive.isNotEmpty())
}

fun testSep31PostTransaction() {
  // Create sender customer
  val senderCustomerRequest =
    GsonUtils.getInstance().fromJson(testCustomer1Json, Sep12PutCustomerRequest::class.java)
  val senderCustomer = sep12Client.putCustomer(senderCustomerRequest)

  // Create receiver customer
  val receiverCustomerRequest =
    GsonUtils.getInstance().fromJson(testCustomer2Json, Sep12PutCustomerRequest::class.java)
  val receiverCustomer = sep12Client.putCustomer(receiverCustomerRequest)

  // Create asset quote
  val quote =
    sep38.postQuote(
      "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
      "10",
      "stellar:JPYC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
    )

  // Post Sep31 transaction.
  val txnRequest = gson.fromJson(postTxnJson, Sep31PostTransactionRequest::class.java)
  txnRequest.senderId = senderCustomer!!.id
  txnRequest.receiverId = receiverCustomer!!.id
  txnRequest.quoteId = quote.id
  sep31Client.postTransaction(txnRequest)
}

fun testBadAsset() {
  val customer =
    GsonUtils.getInstance().fromJson(testCustomer1Json, Sep12PutCustomerRequest::class.java)
  val pr = sep12Client.putCustomer(customer)

  // Post Sep31 transaction.
  val txnRequest = gson.fromJson(postTxnJson, Sep31PostTransactionRequest::class.java)
  txnRequest.assetCode = "bad-asset-code"
  txnRequest.receiverId = pr!!.id
  assertThrows<SepException> { sep31Client.postTransaction(txnRequest) }
}
