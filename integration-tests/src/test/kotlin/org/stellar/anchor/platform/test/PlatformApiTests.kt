package org.stellar.anchor.platform.test

import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.comparator.CustomComparator
import org.stellar.anchor.api.rpc.RpcRequest
import org.stellar.anchor.api.rpc.action.ActionMethod
import org.stellar.anchor.api.rpc.action.NotifyOffchainFundsReceivedRequest
import org.stellar.anchor.api.rpc.action.RequestOffchainFundsRequest
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.platform.Sep24Client
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Sep1Helper.TomlContent

class PlatformApiTests(config: TestConfig, toml: TomlContent, jwt: String) {
  companion object {
    private const val TX_ID = "testTxId"
    private const val JSON_RPC_VERSION = "2.0"
  }

  private val gson = GsonUtils.getInstance()

  private val platformApiClient =
    PlatformApiClient(AuthHelper.forNone(), config.env["platform.server.url"]!!)
  private val sep24Client = Sep24Client(toml.getString("TRANSFER_SERVER_SEP0024"), jwt)

  fun testAll() {
    println("Performing Platform API tests...")
    `send single rpc action`()
    `send batch of rpc actions`()
  }

  private fun `send single rpc action`() {
    val depositRequest = gson.fromJson(DEPOSIT_REQUEST, HashMap::class.java)
    val depositResponse = sep24Client.deposit(depositRequest as HashMap<String, String>)
    val txId = depositResponse.id

    val requestOffchainFundsParams =
      gson.fromJson(REQUEST_OFFCHAIN_FUNDS, RequestOffchainFundsRequest::class.java)
    requestOffchainFundsParams.transactionId = txId
    val rpcRequest =
      RpcRequest.builder()
        .method(ActionMethod.REQUEST_OFFCHAIN_FUNDS.toString())
        .jsonrpc(JSON_RPC_VERSION)
        .params(requestOffchainFundsParams)
        .id(1)
        .build()
    val response = platformApiClient.callRpcAction(listOf(rpcRequest))
    assertEquals(HttpStatus.SC_OK, response.code)
    JSONAssert.assertEquals(
      EXPECTED_RPC_RESPONSE.replace(TX_ID, txId).trimIndent(),
      response.body?.string()?.trimIndent(),
      CustomComparator(
        JSONCompareMode.STRICT,
        Customization("[0].result.started_at") { _, _ -> true },
        Customization("[0].result.updated_at") { _, _ -> true }
      )
    )

    val txResponse = platformApiClient.getTransaction(txId)
    assertEquals(SepTransactionStatus.PENDING_USR_TRANSFER_START, txResponse.status)
  }

  private fun `send batch of rpc actions`() {
    val depositRequest = gson.fromJson(DEPOSIT_REQUEST, HashMap::class.java)
    val depositResponse = sep24Client.deposit(depositRequest as HashMap<String, String>)
    val txId = depositResponse.id

    val requestOffchainFundsParams =
      gson.fromJson(REQUEST_OFFCHAIN_FUNDS, RequestOffchainFundsRequest::class.java)
    val notifyOffchainFundsReceivedParams =
      gson.fromJson(NOTIFY_OFFCHAIN_FUNDS_RECEIVED, NotifyOffchainFundsReceivedRequest::class.java)
    requestOffchainFundsParams.transactionId = txId
    notifyOffchainFundsReceivedParams.transactionId = txId
    val rpcRequest1 =
      RpcRequest.builder()
        .id(1)
        .method(ActionMethod.REQUEST_OFFCHAIN_FUNDS.toString())
        .jsonrpc(JSON_RPC_VERSION)
        .params(requestOffchainFundsParams)
        .build()
    val rpcRequest2 =
      RpcRequest.builder()
        .id(2)
        .method(ActionMethod.NOTIFY_OFFCHAIN_FUNDS_RECEIVED.toString())
        .jsonrpc(JSON_RPC_VERSION)
        .params(notifyOffchainFundsReceivedParams)
        .build()
    val response = platformApiClient.callRpcAction(listOf(rpcRequest1, rpcRequest2))
    assertEquals(HttpStatus.SC_OK, response.code)

    JSONAssert.assertEquals(
      EXPECTED_RPC_BATCH_RESPONSE.replace(TX_ID, txId).trimIndent(),
      response.body?.string()?.trimIndent(),
      CustomComparator(
        JSONCompareMode.STRICT,
        Customization("[0].result.started_at") { _, _ -> true },
        Customization("[0].result.updated_at") { _, _ -> true },
        Customization("[1].result.started_at") { _, _ -> true },
        Customization("[1].result.updated_at") { _, _ -> true }
      )
    )

    val txResponse = platformApiClient.getTransaction(txId)
    assertEquals(SepTransactionStatus.PENDING_ANCHOR, txResponse.status)
  }
}

private const val DEPOSIT_REQUEST =
  """{
    "asset_code": "USDC",
    "asset_issuer": "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
    "lang": "en"
  }"""

private const val REQUEST_OFFCHAIN_FUNDS =
  """{
    "transaction_id": "testTxId",
    "message": "test message",
    "amount_in": {
        "amount": "1",
        "asset": "iso4217:USD"
    },
    "amount_out": {
        "amount": "0.9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    },
    "amount_fee": {
        "amount": "0.1",
        "asset": "iso4217:USD"
    },
    "amount_expected": {
        "amount": "1"
    }
  }"""

private const val NOTIFY_OFFCHAIN_FUNDS_RECEIVED =
  """{
    "transaction_id": "testTxId",
    "message": "test message",
    "amount_in": {
        "amount": "1"
    },
    "amount_out": {
        "amount": "0.9"
    },
    "amount_fee": {
        "amount": "0.1"
    },
    "external_transaction_id": "1"
  }"""

private const val EXPECTED_RPC_RESPONSE =
  """
  [
   {
      "jsonrpc":"2.0",
      "result":{
         "id":"testTxId",
         "sep":"24",
         "kind":"deposit",
         "status":"pending_user_transfer_start",
         "amount_expected":{
            "amount":"1",
            "asset":"stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
         },
         "amount_in":{
            "amount":"1",
            "asset":"iso4217:USD"
         },
         "amount_out":{
            "amount":"0.9",
            "asset":"stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
         },
         "amount_fee":{
            "amount":"0.1",
            "asset":"iso4217:USD"
         },
         "started_at":"2023-07-20T08:57:05.380736Z",
         "updated_at":"2023-07-20T08:57:16.672110400Z",
         "message":"test message",
         "destination_account":"GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
      },
      "id":1
   }
] 
"""

private const val EXPECTED_RPC_BATCH_RESPONSE =
  """
  [
   {
      "jsonrpc":"2.0",
      "result":{
         "id":"testTxId",
         "sep":"24",
         "kind":"deposit",
         "status":"pending_user_transfer_start",
         "amount_expected":{
            "amount":"1",
            "asset":"stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
         },
         "amount_in":{
            "amount":"1",
            "asset":"iso4217:USD"
         },
         "amount_out":{
            "amount":"0.9",
            "asset":"stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
         },
         "amount_fee":{
            "amount":"0.1",
            "asset":"iso4217:USD"
         },
         "started_at":"2023-07-20T09:07:51.007629Z",
         "updated_at":"2023-07-20T09:07:59.425534900Z",
         "message":"test message",
         "destination_account":"GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
      },
      "id":1
   },
   {
      "jsonrpc":"2.0",
      "result":{
         "id":"testTxId",
         "sep":"24",
         "kind":"deposit",
         "status":"pending_anchor",
         "amount_expected":{
            "amount":"1",
            "asset":"stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
         },
         "amount_in":{
            "amount":"1",
            "asset":"iso4217:USD"
         },
         "amount_out":{
            "amount":"0.9",
            "asset":"stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
         },
         "amount_fee":{
            "amount":"0.1",
            "asset":"iso4217:USD"
         },
         "started_at":"2023-07-20T09:07:51.007629Z",
         "updated_at":"2023-07-20T09:07:59.448888600Z",
         "message":"test message",
         "external_transaction_id": "1",
         "destination_account":"GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
      },
      "id":2
   }
] 
"""
