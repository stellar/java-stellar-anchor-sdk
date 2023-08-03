package org.stellar.anchor.platform.test

import com.google.gson.reflect.TypeToken
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.comparator.CustomComparator
import org.stellar.anchor.api.rpc.RpcRequest
import org.stellar.anchor.api.rpc.action.ActionMethod
import org.stellar.anchor.api.rpc.action.ActionMethod.REQUEST_OFFCHAIN_FUNDS
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
    private const val TX_ID_KEY = "TX_ID"
  }

  private val gson = GsonUtils.getInstance()

  private val platformApiClient =
    PlatformApiClient(AuthHelper.forNone(), config.env["platform.server.url"]!!)
  private val sep24Client = Sep24Client(toml.getString("TRANSFER_SERVER_SEP0024"), jwt)

  fun testAll() {
    println("Performing Platform API tests...")
    `deposit short flow`()
    `send single rpc action`()
    `send batch of rpc actions`()
  }

  private fun `send single rpc action`() {
    val depositRequest = gson.fromJson(DEPOSIT_REQUEST, HashMap::class.java)
    val depositResponse = sep24Client.deposit(depositRequest as HashMap<String, String>)
    val txId = depositResponse.id

    val requestOffchainFundsParams =
      gson.fromJson(REQUEST_OFFCHAIN_FUNDS_PARAMS, RequestOffchainFundsRequest::class.java)
    requestOffchainFundsParams.transactionId = txId
    val rpcRequest =
      RpcRequest.builder()
        .method(REQUEST_OFFCHAIN_FUNDS.toString())
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
      gson.fromJson(REQUEST_OFFCHAIN_FUNDS_PARAMS, RequestOffchainFundsRequest::class.java)
    val notifyOffchainFundsReceivedParams =
      gson.fromJson(
        NOTIFY_OFFCHAIN_FUNDS_RECEIVED_PARAMS,
        NotifyOffchainFundsReceivedRequest::class.java
      )
    requestOffchainFundsParams.transactionId = txId
    notifyOffchainFundsReceivedParams.transactionId = txId
    val rpcRequest1 =
      RpcRequest.builder()
        .id(1)
        .method(REQUEST_OFFCHAIN_FUNDS.toString())
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

  private fun `deposit short flow`() {
    val depositRequest = gson.fromJson(DEPOSIT_REQUEST, HashMap::class.java)
    val depositResponse = sep24Client.deposit(depositRequest as HashMap<String, String>)
    val txId = depositResponse.id

    `test flow`(txId, DEPOSIT_SHORT_FLOW_ACTIONS_REQUEST, DEPOSIT_SHORT_FLOW_ACTIONS_RESPONSE)
  }

  private fun `test flow`(txId: String, request: String, response: String) {
    val rpcActionRequestsType = object : TypeToken<List<RpcRequest>>() {}.type
    val rpcActionRequests: List<RpcRequest> =
      gson.fromJson(request.replace(TX_ID_KEY, txId), rpcActionRequestsType)

    val rpcActionResponses = platformApiClient.callRpcAction(rpcActionRequests)

    val expectedResult = response.replace(TX_ID_KEY, txId).trimIndent()
    val actualResult = rpcActionResponses.body?.string()?.replace(TX_ID_KEY, txId)?.trimIndent()

    JSONAssert.assertEquals(
      expectedResult,
      actualResult,
      CustomComparator(
        JSONCompareMode.STRICT,
        Customization("[*].result.started_at") { _, _ -> true },
        Customization("[*].result.updated_at") { _, _ -> true },
        Customization("[*].result.completed_at") { _, _ -> true }
      )
    )
  }
}

private const val DEPOSIT_SHORT_FLOW_ACTIONS_REQUEST =
  """
[
  {
    "id": "1",
    "method": "request_offchain_funds",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 1",
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
    }
  },
  {
    "id": "2",
    "method": "notify_offchain_funds_received",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 2",
      "funds_received_at": "2023-07-04T12:34:56Z",
      "external_transaction_id": "ext-123456",
      "amount_in": {
        "amount": 2
      }
    }
  },
  {
    "id": "3",
    "method": "notify_onchain_funds_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 3",
      "stellar_transaction_id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1"
    }
  }
]
  """

private const val DEPOSIT_SHORT_FLOW_ACTIONS_RESPONSE =
  """
[
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_user_transfer_start",
      "amount_expected": {
        "amount": "1",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
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
      "started_at": "2023-08-03T11:49:36.727322Z",
      "updated_at": "2023-08-03T11:49:58.930151Z",
      "message": "test message 1",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    },
    "id": "1"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_anchor",
      "amount_expected": {
        "amount": "1",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "2",
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
      "started_at": "2023-08-03T11:49:36.727322Z",
      "updated_at": "2023-08-03T11:49:59.991254Z",
      "message": "test message 2",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "external_transaction_id": "ext-123456"
    },
    "id": "2"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "completed",
      "amount_expected": {
        "amount": "1",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "2",
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
      "started_at": "2023-08-03T11:49:36.727322Z",
      "updated_at": "2023-08-03T11:50:01.596766Z",
      "completed_at": "2023-08-03T11:50:01.596771Z",
      "message": "test message 3",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "created_at": "2023-06-22T08:46:56Z",
          "envelope": "AAAAAgAAAABBsSNsYI9mqhg2INua8oEzk88ixjqc/Yiq0/4MNDIcAwAPQkAAAcGcAAAACAAAAAEAAAAAIHqjOgAAAABklDSfAAAAAAAAAAEAAAAAAAAAAQAAAAC9yF4ErTewnyaxhbV7fzgFiY7A8k7xt62CIhYMXt/ovgAAAAFVU0RDAAAAAEI+fQXy7K+/7BkrIVo/G+lq7bjY5wJUq+NBPgIH3layAAAAAAKupUAAAAAAAAAAATQyHAMAAABAUjCaXkOy4VHDpkVwG42lF7ZKK471bMsKSjP2EZtYnBo4e/kYtcVNp+z15EX/qHZBvGWtbFiCBBLXQs7hmu15Cg==",
          "payments": [
            {
              "id": "563306435719169",
              "amount": {
                "amount": "4.5000000",
                "asset": "USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
              },
              "payment_type": "payment",
              "source_account": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6",
              "destination_account": "GC64QXQEVU33BHZGWGC3K637HACYTDWA6JHPDN5NQIRBMDC637UL4F2W"
            }
          ]
        }
      ],
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "external_transaction_id": "ext-123456"
    },
    "id": "3"
  }
]
  """

private const val DEPOSIT_REQUEST =
  """{
    "asset_code": "USDC",
    "asset_issuer": "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
    "lang": "en"
  }"""

private const val REQUEST_OFFCHAIN_FUNDS_PARAMS =
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

private const val NOTIFY_OFFCHAIN_FUNDS_RECEIVED_PARAMS =
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
