package org.stellar.anchor.platform

import org.junit.jupiter.api.Assertions.*
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.event.models.TransactionEvent
import org.stellar.anchor.reference.client.PlatformApiClient
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Sep1Helper

lateinit var platformApiClient: PlatformApiClient

fun platformTestAll(toml: Sep1Helper.TomlContent, jwt: String) {
  println("Performing Platform API tests...")
  sep12Client = Sep12Client(toml.getString("KYC_SERVER"), jwt)
  sep31Client = Sep31Client(toml.getString("DIRECT_PAYMENT_SERVER"), jwt)
  sep38 = Sep38Client(toml.getString("ANCHOR_QUOTE_SERVER"), jwt)
  platformApiClient = PlatformApiClient("http://localhost:8080")

  testHappyPath()
  testHealth()
}

fun testHappyPath() {
  // Create sender customer
  val senderCustomerRequest =
    GsonUtils.getInstance().fromJson(testCustomer1Json, Sep12PutCustomerRequest::class.java)
  val senderCustomer = sep12Client.putCustomer(senderCustomerRequest, TYPE_MULTIPART_FORM_DATA)

  // Create receiver customer
  val receiverCustomerRequest =
    GsonUtils.getInstance().fromJson(testCustomer2Json, Sep12PutCustomerRequest::class.java)
  val receiverCustomer = sep12Client.putCustomer(receiverCustomerRequest)
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
  val postTxResponse = sep31Client.postTransaction(txnRequest)

  val getTxResponse = platformApiClient.getTransaction(postTxResponse.id)
  assertEquals(postTxResponse.id, getTxResponse.id)
  assertEquals(TransactionEvent.Status.PENDING_SENDER.status, getTxResponse.status)
  assertEquals(txnRequest.amount, getTxResponse.amountIn.amount)
  assertTrue(getTxResponse.amountIn.asset.contains(txnRequest.assetCode))
  assertEquals(31, getTxResponse.sep)
}

fun testHealth() {
  val response = platformApiClient.health(listOf("all"))
  assertEquals(response["number_of_checks"], 1.0)
  assertNotNull(response["checks"])
  val checks = response["checks"] as Map<*, *>
  val spo = checks["stellar_payment_observer"] as Map<*, *>
  assertEquals(spo["status"], "green")
  val streams = spo["streams"] as List<Map<*, *>>
  assertEquals(streams[0]["thread_shutdown"], false)
  assertEquals(streams[0]["thread_terminated"], false)
  assertEquals(streams[0]["stopped"], false)
}
