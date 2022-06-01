package org.stellar.anchor.platform

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
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
  // Create customer
  val customer =
    GsonUtils.getInstance().fromJson(testCustomerJson, Sep12PutCustomerRequest::class.java)
  val pr = sep12Client.putCustomer(customer)
  // Post Sep31 transaction.
  val txnRequest = gson.fromJson(postTxnJson, Sep31PostTransactionRequest::class.java)
  txnRequest.receiverId = pr!!.id
  val txnPosted = sep31Client.postTransaction(txnRequest)

  val txnQueried = platformApiClient.getTransaction(txnPosted.id)
  assertEquals(txnPosted.id, txnQueried.id)
  assertEquals(txnQueried.status, "pending_sender")
  assertEquals(txnRequest.amount, txnQueried.amountIn.amount)
  assertEquals(txnRequest.assetCode, txnQueried.amountIn.asset)
  assertEquals(31, txnQueried.sep)
}

fun testHealth() {
  val response = platformApiClient.health(listOf("all"))
  assertEquals(response["number_of_checks"], 1.0)
  Assertions.assertNotNull(response["checks"])
  val checks = response["checks"] as Map<*, *>
  val spo = checks["stellar_payment_observer"] as Map<*, *>
  assertEquals(spo["status"], "green")
  val streams = spo["streams"] as List<Map<*, *>>
  assertEquals(streams[0]["thread_shutdown"], false)
  assertEquals(streams[0]["thread_terminated"], false)
  assertEquals(streams[0]["stopped"], false)
}
