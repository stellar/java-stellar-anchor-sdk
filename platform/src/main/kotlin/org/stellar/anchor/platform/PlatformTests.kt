package org.stellar.anchor.platform

import org.junit.jupiter.api.Assertions.assertEquals
import org.stellar.anchor.dto.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.dto.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Sep1Helper

lateinit var platformApiClient: PlatformApiClient

fun platformTestAll(toml: Sep1Helper.TomlContent, jwt: String) {
  println("Performing Platform API tests...")
  sep12Client = Sep12Client(toml.getString("KYC_SERVER"), jwt)
  sep31Client = Sep31Client(toml.getString("DIRECT_PAYMENT_SERVER"), jwt)
  platformApiClient = PlatformApiClient("http://localhost:8080")

  testHappyPath()
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
