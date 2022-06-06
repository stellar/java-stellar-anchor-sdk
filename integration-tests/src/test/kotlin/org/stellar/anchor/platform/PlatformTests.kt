package org.stellar.anchor.platform

import org.junit.jupiter.api.Assertions.*
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.reference.client.PlatformApiClient
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Sep1Helper

lateinit var platformApiClient: PlatformApiClient

fun platformTestAll(toml: Sep1Helper.TomlContent, jwt: String) {
  println("Performing Platform API tests...")
  sep12Client = Sep12Client(toml.getString("KYC_SERVER"), jwt)
  sep31Client = Sep31Client(toml.getString("DIRECT_PAYMENT_SERVER"), jwt)
  platformApiClient = PlatformApiClient("http://localhost:8080")

  testHappyPath()
  testHealth()
}

fun testHappyPath() {
  // Create sender customer
  val senderCustomerRequest =
    GsonUtils.getInstance().fromJson(testCustomer1Json, Sep12PutCustomerRequest::class.java)
  val senderCustomer = sep12Client.putCustomer(senderCustomerRequest)

  // Create receiver customer
  val receiverCustomerRequest =
    GsonUtils.getInstance().fromJson(testCustomer2Json, Sep12PutCustomerRequest::class.java)
  val receiverCustomer = sep12Client.putCustomer(receiverCustomerRequest)

  // Post Sep31 transaction.
  val txnRequest = gson.fromJson(postTxnJson, Sep31PostTransactionRequest::class.java)
  txnRequest.senderId = senderCustomer!!.id
  txnRequest.receiverId = receiverCustomer!!.id
  val txnPosted = sep31Client.postTransaction(txnRequest)

  val txnQueried = platformApiClient.getTransaction(txnPosted.id)
  assertEquals(txnPosted.id, txnQueried.id)
  assertEquals(txnQueried.status, "pending_sender")
  assertEquals(txnRequest.amount, txnQueried.amountIn.amount)
  assertTrue(txnQueried.amountIn.asset.contains(txnRequest.assetCode))
  assertEquals(31, txnQueried.sep)
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
