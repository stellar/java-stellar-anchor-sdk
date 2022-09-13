package org.stellar.anchor.platform

import java.time.temporal.ChronoUnit.SECONDS
import org.junit.jupiter.api.Assertions.*
import org.stellar.anchor.api.platform.PatchTransactionRequest
import org.stellar.anchor.api.platform.PatchTransactionsRequest
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.api.sep.sep12.Sep12Status
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.auth.JwtService
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

  val platformToAnchorJwtService = JwtService("myAnchorToPlatformSecret")
  val authHelper =
    AuthHelper.forJwtToken(platformToAnchorJwtService, 900000, "http://localhost:8081")
  platformApiClient = PlatformApiClient(authHelper, "http://localhost:8080")

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

  // POST Sep31 transaction
  val txnRequest = gson.fromJson(postTxnJson, Sep31PostTransactionRequest::class.java)
  txnRequest.senderId = senderCustomer!!.id
  txnRequest.receiverId = receiverCustomer!!.id
  txnRequest.quoteId = quote.id
  val postTxResponse = sep31Client.postTransaction(txnRequest)

  // GET platformAPI transaction
  val getTxResponse = platformApiClient.getTransaction(postTxResponse.id)
  assertEquals(postTxResponse.id, getTxResponse.id)
  assertEquals(TransactionEvent.Status.PENDING_SENDER.status, getTxResponse.status)
  assertEquals(txnRequest.amount, getTxResponse.amountIn.amount)
  assertTrue(getTxResponse.amountIn.asset.contains(txnRequest.assetCode))
  assertEquals(31, getTxResponse.sep)

  // PATCH transaction status to COMPLETED through platformAPI
  val patchTxRequest =
    PatchTransactionRequest.builder()
      .id(getTxResponse.id)
      .status(TransactionEvent.Status.COMPLETED.status)
      .amountOut(Amount(quote.buyAmount, quote.buyAsset))
      .build()
  val patchTxResponse =
    platformApiClient.patchTransaction(
      PatchTransactionsRequest.builder().records(listOf(patchTxRequest)).build()
    )
  assertEquals(1, patchTxResponse.records.size)
  val patchedTx = patchTxResponse.records[0]
  assertEquals(getTxResponse.id, patchedTx.id)
  assertEquals(TransactionEvent.Status.COMPLETED.status, patchedTx.status)
  assertEquals(quote.buyAmount, patchedTx.amountOut.amount)
  assertEquals(quote.buyAsset, patchedTx.amountOut.asset)
  assertEquals(31, getTxResponse.sep)
}

fun testHealth() {
  val response = platformApiClient.health(listOf("all"))
  assertEquals(5, response.size)
  assertNotNull(response["checks"])
  assertEquals(0.0, response["number_of_checks"])
  assertNotNull(response["started_at"])
  assertNotNull(response["elapsed_time_ms"])
  assertNotNull(response["number_of_checks"])
  assertNotNull(response["version"])
}

fun testSep31UnhappyPath() {
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

  // POST SEP-31 transaction
  val txnRequest = gson.fromJson(postTxnJson, Sep31PostTransactionRequest::class.java)
  txnRequest.senderId = senderCustomer!!.id
  txnRequest.receiverId = receiverCustomer!!.id
  txnRequest.quoteId = quote.id
  val postTxResponse = sep31Client.postTransaction(txnRequest)

  // GET platformAPI transaction
  val getTxResponse = platformApiClient.getTransaction(postTxResponse.id)
  assertEquals(postTxResponse.id, getTxResponse.id)
  assertEquals(TransactionEvent.Status.PENDING_SENDER.status, getTxResponse.status)
  assertEquals(txnRequest.amount, getTxResponse.amountIn.amount)
  assertTrue(getTxResponse.amountIn.asset.contains(txnRequest.assetCode))
  assertEquals(31, getTxResponse.sep)
  assertNull(getTxResponse.completedAt)
  assertNotNull(getTxResponse.startedAt)
  assertTrue(getTxResponse.updatedAt >= getTxResponse.startedAt)

  // Modify the customer by erasing its clabe_number to simulate an invalid clabe_number
  sep12Client.invalidateCustomerClabe(receiverCustomer.id)
  var updatedReceiverCustomer = sep12Client.getCustomer(receiverCustomer.id, "sep31-receiver")
  assertEquals(Sep12Status.NEEDS_INFO, updatedReceiverCustomer?.status)
  assertNotNull(updatedReceiverCustomer?.fields?.get("clabe_number"))
  assertNull(updatedReceiverCustomer?.providedFields?.get("clabe_number"))

  // PATCH {platformAPI}/transaction status to PENDING_CUSTOMER_INFO_UPDATE, since the clabe_number
  // was invalidated.
  var patchTxRequest =
    PatchTransactionRequest.builder()
      .id(getTxResponse.id)
      .status(TransactionEvent.Status.PENDING_CUSTOMER_INFO_UPDATE.status)
      .message("The receiving customer clabe_number is invalid!")
      .build()
  var patchTxResponse =
    platformApiClient.patchTransaction(
      PatchTransactionsRequest.builder().records(listOf(patchTxRequest)).build()
    )
  assertEquals(1, patchTxResponse.records.size)
  var patchedTx = patchTxResponse.records[0]
  assertEquals(getTxResponse.id, patchedTx.id)
  assertEquals(TransactionEvent.Status.PENDING_CUSTOMER_INFO_UPDATE.status, patchedTx.status)
  assertEquals(31, patchedTx.sep)
  assertEquals("The receiving customer clabe_number is invalid!", patchedTx.message)
  assertTrue(patchedTx.updatedAt > patchedTx.startedAt)

  // GET SEP-31 transaction should return PENDING_CUSTOMER_INFO_UPDATE with a message
  var gotSep31TxResponse = sep31Client.getTransaction(postTxResponse.id)
  assertEquals(postTxResponse.id, gotSep31TxResponse.transaction.id)
  assertEquals(
    TransactionEvent.Status.PENDING_CUSTOMER_INFO_UPDATE.status,
    gotSep31TxResponse.transaction.status
  )
  assertEquals(
    "The receiving customer clabe_number is invalid!",
    gotSep31TxResponse.transaction.requiredInfoMessage
  )
  assertNull(gotSep31TxResponse.transaction.completedAt)

  // PUT sep12/customer with the correct clabe_number
  sep12Client.putCustomer(
    Sep12PutCustomerRequest.builder().id(receiverCustomer.id).clabeNumber("5678").build()
  )
  updatedReceiverCustomer = sep12Client.getCustomer(receiverCustomer.id, "sep31-receiver")
  assertEquals(Sep12Status.ACCEPTED, updatedReceiverCustomer?.status)
  assertNull(updatedReceiverCustomer?.fields?.get("clabe_number"))
  assertNotNull(updatedReceiverCustomer?.providedFields?.get("clabe_number"))

  // PATCH {platformAPI}/transaction status to COMPLETED, since the clabe_number was updated
  // correctly.
  patchTxRequest =
    PatchTransactionRequest.builder()
      .id(getTxResponse.id)
      .status(TransactionEvent.Status.COMPLETED.status)
      .build()
  patchTxResponse =
    platformApiClient.patchTransaction(
      PatchTransactionsRequest.builder().records(listOf(patchTxRequest)).build()
    )
  assertEquals(1, patchTxResponse.records.size)
  patchedTx = patchTxResponse.records[0]
  assertEquals(getTxResponse.id, patchedTx.id)
  assertEquals(TransactionEvent.Status.COMPLETED.status, patchedTx.status)
  assertEquals(31, patchedTx.sep)
  assertNull(patchedTx.message)
  assertTrue(patchedTx.startedAt < patchedTx.updatedAt)
  assertEquals(patchedTx.updatedAt, patchedTx.completedAt)

  // GET SEP-31 transaction should return COMPLETED with no message
  gotSep31TxResponse = sep31Client.getTransaction(postTxResponse.id)
  assertEquals(postTxResponse.id, gotSep31TxResponse.transaction.id)
  assertEquals(TransactionEvent.Status.COMPLETED.status, gotSep31TxResponse.transaction.status)
  assertNull(gotSep31TxResponse.transaction.requiredInfoMessage)
  assertEquals(
    patchedTx.completedAt.truncatedTo(SECONDS),
    gotSep31TxResponse.transaction.completedAt.truncatedTo(SECONDS)
  )
}
