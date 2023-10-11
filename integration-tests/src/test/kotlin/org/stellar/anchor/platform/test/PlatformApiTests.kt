package org.stellar.anchor.platform.test

import com.google.gson.reflect.TypeToken
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.comparator.CustomComparator
import org.stellar.anchor.api.rpc.RpcRequest
import org.stellar.anchor.api.rpc.method.NotifyOffchainFundsReceivedRequest
import org.stellar.anchor.api.rpc.method.RequestOffchainFundsRequest
import org.stellar.anchor.api.rpc.method.RpcMethod
import org.stellar.anchor.api.rpc.method.RpcMethod.REQUEST_OFFCHAIN_FUNDS
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.platform.Sep12Client
import org.stellar.anchor.platform.Sep24Client
import org.stellar.anchor.platform.Sep31Client
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Sep1Helper.TomlContent

class PlatformApiTests(config: TestConfig, toml: TomlContent, jwt: String) {
  companion object {
    private const val TX_ID = "testTxId"
    private const val JSON_RPC_VERSION = "2.0"
    private const val TX_ID_KEY = "TX_ID"
    private const val RECEIVER_ID_KEY = "RECEIVER_ID"
    private const val SENDER_ID_KEY = "SENDER_ID"
  }

  private val gson = GsonUtils.getInstance()

  private val platformApiClient =
    PlatformApiClient(AuthHelper.forNone(), config.env["platform.server.url"]!!)
  private val sep12Client = Sep12Client(toml.getString("KYC_SERVER"), jwt)
  private val sep24Client = Sep24Client(toml.getString("TRANSFER_SERVER_SEP0024"), jwt)
  private val sep31Client = Sep31Client(toml.getString("DIRECT_PAYMENT_SERVER"), jwt)

  fun testAll() {
    println("Performing Platform API tests...")
    `SEP-24 deposit complete short flow`()
    `SEP-24 deposit complete full with trust flow`()
    `SEP-24 deposit complete full with recovery flow`()
    `SEP-24 deposit complete short partial refund flow`()
    `SEP-24 withdraw complete short flow`()
    `SEP-24 withdraw complete full via pending external`()
    `SEP-24 withdraw complete full via pending user`()
    `SEP-24 withdraw full refund`()
    `SEP-31 refunded short`()
    `SEP-31 complete full with recovery`()
    `validations and errors`()
    `send single rpc request`()
    `send batch of rpc requests`()
  }

  private fun `send single rpc request`() {
    val depositRequest = gson.fromJson(SEP_24_DEPOSIT_REQUEST, HashMap::class.java)

    @Suppress("UNCHECKED_CAST")
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
    val response = platformApiClient.sendRpcRequest(listOf(rpcRequest))
    assertEquals(HttpStatus.SC_OK, response.code)
    JSONAssert.assertEquals(
      EXPECTED_RPC_RESPONSE.replace(TX_ID, txId).trimIndent(),
      response.body?.string()?.trimIndent(),
      CustomComparator(
        JSONCompareMode.STRICT,
        Customization("[*].result.started_at") { _, _ -> true },
        Customization("[*].result.updated_at") { _, _ -> true }
      )
    )

    val txResponse = platformApiClient.getTransaction(txId)
    assertEquals(SepTransactionStatus.PENDING_USR_TRANSFER_START, txResponse.status)
  }

  private fun `send batch of rpc requests`() {
    val depositRequest = gson.fromJson(SEP_24_DEPOSIT_REQUEST, HashMap::class.java)

    @Suppress("UNCHECKED_CAST")
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
        .method(RpcMethod.NOTIFY_OFFCHAIN_FUNDS_RECEIVED.toString())
        .jsonrpc(JSON_RPC_VERSION)
        .params(notifyOffchainFundsReceivedParams)
        .build()
    val response = platformApiClient.sendRpcRequest(listOf(rpcRequest1, rpcRequest2))
    assertEquals(HttpStatus.SC_OK, response.code)

    JSONAssert.assertEquals(
      EXPECTED_RPC_BATCH_RESPONSE.replace(TX_ID, txId).trimIndent(),
      response.body?.string()?.trimIndent(),
      CustomComparator(
        JSONCompareMode.STRICT,
        Customization("[*].result.started_at") { _, _ -> true },
        Customization("[*].result.updated_at") { _, _ -> true }
      )
    )

    val txResponse = platformApiClient.getTransaction(txId)
    assertEquals(SepTransactionStatus.PENDING_ANCHOR, txResponse.status)
  }

  /**
   * 1. incomplete -> request_offchain_funds
   * 2. pending_user_transfer_start -> notify_offchain_funds_received
   * 3. pending_anchor -> notify_onchain_funds_sent
   * 4. completed
   */
  private fun `SEP-24 deposit complete short flow`() {
    `test deposit flow`(
      SEP_24_DEPOSIT_COMPLETE_SHORT_FLOW_ACTION_REQUESTS,
      SEP_24_DEPOSIT_COMPLETE_SHORT_FLOW_ACTION_RESPONSES
    )
  }

  /**
   * 1. incomplete -> notify_interactive_flow_completed
   * 2. pending_anchor -> request_offchain_funds
   * 3. pending_user_transfer_start -> notify_offchain_funds_received
   * 4. pending_anchor -> request_trust
   * 5. pending_trust -> notify_trust_set
   * 6. pending_anchor -> notify_onchain_funds_sent
   * 7. completed
   */
  private fun `SEP-24 deposit complete full with trust flow`() {
    `test deposit flow`(
      SEP_24_DEPOSIT_COMPLETE_FULL_WITH_TRUST_FLOW_ACTION_REQUESTS,
      SEP_24_DEPOSIT_COMPLETE_FULL_WITH_TRUST_FLOW_ACTION_RESPONSES
    )
  }

  /**
   * 1. incomplete -> request_offchain_funds
   * 2. pending_user_transfer_start -> notify_offchain_funds_received
   * 3. pending_anchor -> notify_transaction_error
   * 4. error -> notify_transaction_recovery
   * 5. pending_anchor -> notify_onchain_funds_sent
   * 6. completed
   */
  private fun `SEP-24 deposit complete full with recovery flow`() {
    `test deposit flow`(
      SEP_24_DEPOSIT_COMPLETE_FULL_WITH_RECOVERY_FLOW_ACTION_REQUESTS,
      SEP_24_DEPOSIT_COMPLETE_FULL_WITH_RECOVERY_FLOW_ACTION_RESPONSES
    )
  }

  /**
   * 1. incomplete -> request_offchain_funds
   * 2. pending_user_transfer_start -> notify_offchain_funds_received
   * 3. pending_anchor -> notify_refund_pending
   * 4. pending_external -> notify_refund_sent
   * 5. pending_anchor -> notify_onchain_funds_sent
   * 6. completed
   */
  private fun `SEP-24 deposit complete short partial refund flow`() {
    `test deposit flow`(
      SEP_24_DEPOSIT_COMPLETE_SHORT_PARTIAL_REFUND_FLOW_ACTION_REQUESTS,
      SEP_24_DEPOSIT_COMPLETE_SHORT_PARTIAL_REFUND_FLOW_ACTION_RESPONSES
    )
  }

  /**
   * 1. incomplete -> request_onchain_funds
   * 2. pending_user_transfer_start -> notify_onchain_funds_received
   * 3. pending_anchor -> notify_offchain_funds_sent
   * 4. completed
   */
  private fun `SEP-24 withdraw complete short flow`() {
    `test withdraw flow`(
      SEP_24_WITHDRAW_COMPLETE_SHORT_FLOW_ACTION_REQUESTS,
      SEP_24_WITHDRAW_COMPLETE_SHORT_FLOW_ACTION_RESPONSES
    )
  }

  /**
   * 1. incomplete -> request_onchain_funds
   * 2. pending_user_transfer_start -> notify_onchain_funds_received
   * 3. pending_anchor -> notify_offchain_funds_pending
   * 4. pending_external -> notify_offchain_funds_sent
   * 5. completed
   */
  private fun `SEP-24 withdraw complete full via pending external`() {
    `test withdraw flow`(
      SEP_24_WITHDRAW_COMPLETE_FULL_VIA_PENDING_EXTERNAL_FLOW_ACTION_REQUESTS,
      SEP_24_WITHDRAW_COMPLETE_FULL_VIA_PENDING_EXTERNAL_FLOW_ACTION_RESPONSES
    )
  }

  /**
   * 1. incomplete -> request_onchain_funds
   * 2. pending_user_transfer_start -> notify_onchain_funds_received
   * 3. pending_anchor -> notify_offchain_funds_available
   * 4. pending_user_transfer_complete -> notify_offchain_funds_sent
   * 5. completed
   */
  private fun `SEP-24 withdraw complete full via pending user`() {
    `test withdraw flow`(
      SEP_24_WITHDRAW_COMPLETE_FULL_VIA_PENDING_USER_FLOW_ACTION_REQUESTS,
      SEP_24_WITHDRAW_COMPLETE_FULL_VIA_PENDING_USER_FLOW_ACTION_RESPONSES
    )
  }

  /**
   * 1. incomplete -> request_onchain_funds
   * 2. pending_user_transfer_start -> notify_onchain_funds_received
   * 3. pending_anchor -> notify_refund_sent
   * 4. refunded
   */
  private fun `SEP-24 withdraw full refund`() {
    `test withdraw flow`(
      SEP_24_WITHDRAW_FULL_REFUND_FLOW_ACTION_REQUESTS,
      SEP_24_WITHDRAW_FULL_REFUND_FLOW_ACTION_RESPONSES
    )
  }

  /**
   * 1. pending_sender -> notify_onchain_funds_received
   * 2. pending_receiver -> notify_refund_sent
   * 3. refunded
   */
  private fun `SEP-31 refunded short`() {
    `test receive flow`(
      SEP_31_RECEIVE_REFUNDED_SHORT_FLOW_ACTION_REQUESTS,
      SEP_31_RECEIVE_REFUNDED_SHORT_FLOW_ACTION_RESPONSES
    )
  }

  /**
   * 1. pending_sender -> notify_onchain_funds_received
   * 2. pending_receiver -> request_customer_info_update
   * 3. pending_customer_info_update -> notify_customer_info_updated
   * 4. pending_receiver -> notify_transaction_error
   * 5. error -> notify_transaction_recovery
   * 6. pending_receiver -> notify_offchain_funds_pending
   * 7. pending_external -> notify_offchain_funds_sent
   * 8. completed
   */
  private fun `SEP-31 complete full with recovery`() {
    `test receive flow`(
      SEP_31_RECEIVE_COMPLETE_FULL_WITH_RECOVERY_FLOW_ACTION_REQUESTS,
      SEP_31_RECEIVE_COMPLETE_FULL_WITH_RECOVERY_FLOW_ACTION_RESPONSES
    )
  }

  private fun `validations and errors`() {
    `test deposit flow`(VALIDATIONS_AND_ERRORS_REQUESTS, VALIDATIONS_AND_ERRORS_RESPONSES)
  }

  private fun `test deposit flow`(actionRequests: String, actionResponse: String) {
    val depositRequest = gson.fromJson(SEP_24_DEPOSIT_FLOW_REQUEST, HashMap::class.java)

    @Suppress("UNCHECKED_CAST")
    val depositResponse = sep24Client.deposit(depositRequest as HashMap<String, String>)
    `test flow`(depositResponse.id, actionRequests, actionResponse)
  }

  private fun `test receive flow`(actionRequests: String, actionResponses: String) {
    val receiverCustomerRequest =
      GsonUtils.getInstance().fromJson(CUSTOMER_1, Sep12PutCustomerRequest::class.java)
    val receiverCustomer = sep12Client.putCustomer(receiverCustomerRequest)
    val senderCustomerRequest =
      GsonUtils.getInstance().fromJson(CUSTOMER_2, Sep12PutCustomerRequest::class.java)
    val senderCustomer = sep12Client.putCustomer(senderCustomerRequest)

    val receiveRequestJson =
      SEP_31_RECEIVE_FLOW_REQUEST.replace(RECEIVER_ID_KEY, receiverCustomer!!.id)
        .replace(SENDER_ID_KEY, senderCustomer!!.id)
    val receiveRequest = gson.fromJson(receiveRequestJson, Sep31PostTransactionRequest::class.java)
    val receiveResponse = sep31Client.postTransaction(receiveRequest)

    val updatedActionRequests =
      actionRequests
        .replace(RECEIVER_ID_KEY, receiverCustomer.id)
        .replace(SENDER_ID_KEY, senderCustomer.id)
    val updatedActionResponses =
      actionResponses
        .replace(RECEIVER_ID_KEY, receiverCustomer.id)
        .replace(SENDER_ID_KEY, senderCustomer.id)

    `test flow`(receiveResponse.id, updatedActionRequests, updatedActionResponses)
  }

  private fun `test withdraw flow`(actionRequests: String, actionResponse: String) {
    val withdrawRequest = gson.fromJson(SEP_24_WITHDRAW_FLOW_REQUEST, HashMap::class.java)

    @Suppress("UNCHECKED_CAST")
    val withdrawResponse = sep24Client.withdraw(withdrawRequest as HashMap<String, String>)
    `test flow`(withdrawResponse.id, actionRequests, actionResponse)
  }

  private fun `test flow`(txId: String, actionRequests: String, actionResponses: String) {
    val rpcActionRequestsType = object : TypeToken<List<RpcRequest>>() {}.type
    val rpcActionRequests: List<RpcRequest> =
      gson.fromJson(actionRequests.replace(TX_ID_KEY, txId), rpcActionRequestsType)

    val rpcActionResponses = platformApiClient.sendRpcRequest(rpcActionRequests)

    val expectedResult = actionResponses.replace(TX_ID_KEY, txId).trimIndent()
    val actualResult = rpcActionResponses.body?.string()?.trimIndent()

    JSONAssert.assertEquals(
      expectedResult,
      actualResult,
      CustomComparator(
        JSONCompareMode.STRICT,
        Customization("[*].result.started_at") { _, _ -> true },
        Customization("[*].result.updated_at") { _, _ -> true },
        Customization("[*].result.completed_at") { _, _ -> true },
        Customization("[*].result.memo") { _, _ -> true },
        Customization("[*].result.stellar_transactions[*].memo") { _, _ -> true }
      )
    )
  }
}

private const val SEP_24_DEPOSIT_COMPLETE_SHORT_FLOW_ACTION_REQUESTS =
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
        "amount": "100",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "95",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "100"
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
        "amount": "100"
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

private const val SEP_24_DEPOSIT_COMPLETE_SHORT_FLOW_ACTION_RESPONSES =
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
        "amount": "100",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "100",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "95",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T13:16:01.810865Z",
      "updated_at": "2023-08-03T13:16:03.309042Z",
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
        "amount": "100",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "100",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "95",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T13:16:01.810865Z",
      "updated_at": "2023-08-03T13:16:04.486424Z",
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
        "amount": "100",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "100",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "95",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T13:16:01.810865Z",
      "updated_at": "2023-08-03T13:16:06.158111Z",
      "completed_at": "2023-08-03T13:16:06.158118Z",
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

private const val SEP_24_DEPOSIT_COMPLETE_FULL_WITH_TRUST_FLOW_ACTION_REQUESTS =
  """
[
  {
    "id": "1",
    "method": "notify_interactive_flow_completed",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 1",
      "amount_in": {
        "amount": "100",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "95",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "3"
      }
    }
  },
  {
    "id": "2",
    "method": "request_offchain_funds",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 2",
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "10.11"
      }
    }
  },
  {
  "id": "3",
  "method": "notify_offchain_funds_received",
  "jsonrpc": "2.0",
  "params": {
    "transaction_id": "TX_ID",
    "message": "test message 3",
    "funds_received_at": "2023-07-04T12:34:56Z",
    "external_transaction_id": "ext-123456",
    "amount_in": {
        "amount": "10.11"
      },
      "amount_out": {
        "amount": "9"
      },
      "amount_fee": {
        "amount": "1.11"
      },
      "amount_expected": {
        "amount": "10.11"
      }
    }
  },
  {
    "id": "4",
    "method": "request_trust",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 4"
    }
  },
  {
    "id": "5",
    "method": "notify_trust_set",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 5"
    }
  },
  {
    "id": "6",
    "method": "notify_onchain_funds_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 6",
      "stellar_transaction_id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1"
    }
  }
]
  """

private const val SEP_24_DEPOSIT_COMPLETE_FULL_WITH_TRUST_FLOW_ACTION_RESPONSES =
  """
[
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_anchor",
      "amount_expected": {
        "amount": "3",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "100",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "95",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T13:24:23.164990Z",
      "updated_at": "2023-08-03T13:24:24.202429Z",
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
      "status": "pending_user_transfer_start",
      "amount_expected": {
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T13:24:23.164990Z",
      "updated_at": "2023-08-03T13:24:25.245103Z",
      "message": "test message 2",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    },
    "id": "2"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_anchor",
      "amount_expected": {
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T13:24:23.164990Z",
      "updated_at": "2023-08-03T13:24:26.274735Z",
      "message": "test message 3",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "external_transaction_id": "ext-123456"
    },
    "id": "3"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_trust",
      "amount_expected": {
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T13:24:23.164990Z",
      "updated_at": "2023-08-03T13:24:27.297727Z",
      "message": "test message 4",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "external_transaction_id": "ext-123456"
    },
    "id": "4"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_anchor",
      "amount_expected": {
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T13:24:23.164990Z",
      "updated_at": "2023-08-03T13:24:28.344421Z",
      "message": "test message 5",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "external_transaction_id": "ext-123456"
    },
    "id": "5"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "completed",
      "amount_expected": {
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T13:24:23.164990Z",
      "updated_at": "2023-08-03T13:24:30.907307Z",
      "completed_at": "2023-08-03T13:24:30.907312Z",
      "message": "test message 6",
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
    "id": "6"
  }
]
  """

private const val SEP_24_DEPOSIT_COMPLETE_FULL_WITH_RECOVERY_FLOW_ACTION_REQUESTS =
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
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "10.11"
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
        "amount": "10.11"
      },
      "amount_out": {
        "amount": "9"
      },
      "amount_fee": {
        "amount": "1.11"
      },
      "amount_expected": {
        "amount": "10.11"
      }
    }
  },
  {
    "id": "3",
    "method": "notify_transaction_error",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 3"
    }
  },
  {
    "id": "4",
    "method": "notify_transaction_recovery",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 4"
    }
  },
  {
    "id": "5",
    "method": "notify_onchain_funds_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 5",
      "stellar_transaction_id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1"
    }
  }
]
  """

private const val SEP_24_DEPOSIT_COMPLETE_FULL_WITH_RECOVERY_FLOW_ACTION_RESPONSES =
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
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T14:07:19.840308Z",
      "updated_at": "2023-08-03T14:07:20.883323Z",
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
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T14:07:19.840308Z",
      "updated_at": "2023-08-03T14:07:21.897462Z",
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
      "status": "error",
      "amount_expected": {
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T14:07:19.840308Z",
      "updated_at": "2023-08-03T14:07:22.924959Z",
      "message": "test message 3",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "external_transaction_id": "ext-123456"
    },
    "id": "3"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_anchor",
      "amount_expected": {
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T14:07:19.840308Z",
      "updated_at": "2023-08-03T14:07:23.977509Z",
      "message": "test message 4",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "external_transaction_id": "ext-123456"
    },
    "id": "4"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "completed",
      "amount_expected": {
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T14:07:19.840308Z",
      "updated_at": "2023-08-03T14:07:25.181690Z",
      "completed_at": "2023-08-03T14:07:25.181694Z",
      "message": "test message 5",
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
    "id": "5"
  }
]
  """

private const val SEP_24_DEPOSIT_COMPLETE_SHORT_PARTIAL_REFUND_FLOW_ACTION_REQUESTS =
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
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "10.11"
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
        "amount": "1000.11"
      },
      "amount_out": {
        "amount": "9"
      },
      "amount_fee": {
        "amount": "1.11"
      },
      "amount_expected": {
        "amount": "10.11"
      }
    }
  },
  {
    "id": "3",
    "method": "notify_refund_pending",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 3",
      "refund": {
        "id": "123456",
        "amount": {
          "amount": "989.11",
          "asset": "iso4217:USD"
        },
        "amount_fee": {
          "amount": "1",
          "asset": "iso4217:USD"
        }
      }
    }
  },
  {
    "id": "4",
    "method": "notify_refund_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 4",
      "refund": {
        "id": "123456",
        "amount": {
          "amount": "989.11",
          "asset": "iso4217:USD"
        },
        "amount_fee": {
          "amount": "1",
          "asset": "iso4217:USD"
        }
      }
    }
  },
  {
    "id": "5",
    "method": "notify_onchain_funds_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 5",
      "stellar_transaction_id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1"
    }
  }
]    
  """

private const val SEP_24_DEPOSIT_COMPLETE_SHORT_PARTIAL_REFUND_FLOW_ACTION_RESPONSES =
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
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T14:32:55.369869Z",
      "updated_at": "2023-08-03T14:32:56.401608Z",
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
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "1000.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T14:32:55.369869Z",
      "updated_at": "2023-08-03T14:32:57.411722Z",
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
      "status": "pending_external",
      "amount_expected": {
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "1000.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T14:32:55.369869Z",
      "updated_at": "2023-08-03T14:32:58.452076Z",
      "message": "test message 3",
      "refunds": {
        "amount_refunded": {
          "amount": "990.11",
          "asset": "iso4217:USD"
        },
        "amount_fee": {
          "amount": "1",
          "asset": "iso4217:USD"
        },
        "payments": [
          {
            "id": "123456",
            "id_type": "stellar",
            "amount": {
              "amount": "989.11",
              "asset": "iso4217:USD"
            },
            "fee": {
              "amount": "1",
              "asset": "iso4217:USD"
            }
          }
        ]
      },
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "external_transaction_id": "ext-123456"
    },
    "id": "3"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_anchor",
      "amount_expected": {
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "1000.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T14:32:55.369869Z",
      "updated_at": "2023-08-03T14:32:59.512350Z",
      "message": "test message 4",
      "refunds": {
        "amount_refunded": {
          "amount": "990.11",
          "asset": "iso4217:USD"
        },
        "amount_fee": {
          "amount": "1",
          "asset": "iso4217:USD"
        },
        "payments": [
          {
            "id": "123456",
            "id_type": "stellar",
            "amount": {
              "amount": "989.11",
              "asset": "iso4217:USD"
            },
            "fee": {
              "amount": "1",
              "asset": "iso4217:USD"
            }
          }
        ]
      },
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "external_transaction_id": "ext-123456"
    },
    "id": "4"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "completed",
      "amount_expected": {
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "1000.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-03T14:32:55.369869Z",
      "updated_at": "2023-08-03T14:33:00.668544Z",
      "completed_at": "2023-08-03T14:33:00.668548Z",
      "message": "test message 5",
      "refunds": {
        "amount_refunded": {
          "amount": "990.11",
          "asset": "iso4217:USD"
        },
        "amount_fee": {
          "amount": "1",
          "asset": "iso4217:USD"
        },
        "payments": [
          {
            "id": "123456",
            "id_type": "stellar",
            "amount": {
              "amount": "989.11",
              "asset": "iso4217:USD"
            },
            "fee": {
              "amount": "1",
              "asset": "iso4217:USD"
            }
          }
        ]
      },
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
    "id": "5"
  }
]   
  """

private const val SEP_24_WITHDRAW_COMPLETE_SHORT_FLOW_ACTION_REQUESTS =
  """
[
  {
    "id": "1",
    "method": "request_onchain_funds",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 1",
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_expected": {
        "amount": "100"
      }
    }
  },
  {
    "id": "2",
    "method": "notify_onchain_funds_received",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 2",
      "stellar_transaction_id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1"
    }
  },
  {
    "id": "3",
    "method": "notify_offchain_funds_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 3",
      "external_transaction_id": "ext-123456"
    }
  }
]
  """

private const val SEP_24_WITHDRAW_COMPLETE_SHORT_FLOW_ACTION_RESPONSES =
  """
[
  {
    "jsonrpc": "2.0",
    "result": {
      "id": TX_ID,
      "sep": "24",
      "kind": "withdrawal",
      "status": "pending_user_transfer_start",
      "amount_expected": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-04T09:30:42.895791Z",
      "updated_at": "2023-08-04T09:30:43.957105200Z",
      "message": "test message 1",
      "source_account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
      "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
      "memo_type": "hash"
    },
    "id": "1"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": TX_ID,
      "sep": "24",
      "kind": "withdrawal",
      "status": "pending_anchor",
      "amount_expected": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-04T09:30:42.895791Z",
      "updated_at": "2023-08-04T09:30:45.138455Z",
      "message": "test message 2",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
          "memo_type": "hash",
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
      "source_account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
      "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
      "memo_type": "hash"
    },
    "id": "2"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": TX_ID,
      "sep": "24",
      "kind": "withdrawal",
      "status": "completed",
      "amount_expected": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-04T09:30:42.895791Z",
      "updated_at": "2023-08-04T09:30:46.190117600Z",
      "completed_at": "2023-08-04T09:30:46.190117600Z",
      "message": "test message 3",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
          "memo_type": "hash",
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
      "source_account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
      "external_transaction_id": "ext-123456",
      "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
      "memo_type": "hash"
    },
    "id": "3"
  }
]
"""

private const val SEP_24_WITHDRAW_COMPLETE_FULL_VIA_PENDING_EXTERNAL_FLOW_ACTION_REQUESTS =
  """
[
  {
    "id": "1",
    "method": "request_onchain_funds",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 1",
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_expected": {
        "amount": "100"
      }
    }
  },
  {
    "id": "2",
    "method": "notify_onchain_funds_received",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 2",
      "stellar_transaction_id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1"
    }
  },
  {
    "id": "3",
    "method": "notify_offchain_funds_pending",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 3",
      "external_transaction_id": "ext-123456"
    }
  },
  {
    "id": "4",
    "method": "notify_offchain_funds_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 4",
      "external_transaction_id": "ext-123456"
    }
  }
]
  """

private const val SEP_24_WITHDRAW_COMPLETE_FULL_VIA_PENDING_EXTERNAL_FLOW_ACTION_RESPONSES =
  """
[
  {
    "jsonrpc": "2.0",
    "result": {
      "id": TX_ID,
      "sep": "24",
      "kind": "withdrawal",
      "status": "pending_user_transfer_start",
      "amount_expected": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-04T09:30:42.895791Z",
      "updated_at": "2023-08-04T09:30:43.957105200Z",
      "message": "test message 1",
      "source_account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
      "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
      "memo_type": "hash"
    },
    "id": "1"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": TX_ID,
      "sep": "24",
      "kind": "withdrawal",
      "status": "pending_anchor",
      "amount_expected": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-04T09:30:42.895791Z",
      "updated_at": "2023-08-04T09:30:45.138455Z",
      "message": "test message 2",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
          "memo_type": "hash",
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
      "source_account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
      "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
      "memo_type": "hash"
    },
    "id": "2"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": TX_ID,
      "sep": "24",
      "kind": "withdrawal",
      "status": "pending_external",
      "amount_expected": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-04T09:30:42.895791Z",
      "updated_at": "2023-08-04T09:30:46.190117600Z",
      "message": "test message 3",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
          "memo_type": "hash",
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
      "source_account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
      "external_transaction_id": "ext-123456",
      "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
      "memo_type": "hash"
    },
    "id": "3"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": TX_ID,
      "sep": "24",
      "kind": "withdrawal",
      "status": "completed",
      "amount_expected": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-04T09:30:42.895791Z",
      "updated_at": "2023-08-04T09:30:46.190117600Z",
      "completed_at": "2023-08-04T09:30:46.190117600Z",
      "message": "test message 4",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
          "memo_type": "hash",
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
      "source_account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
      "external_transaction_id": "ext-123456",
      "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
      "memo_type": "hash"
    },
    "id": "4"
  }
]
"""

private const val SEP_24_WITHDRAW_COMPLETE_FULL_VIA_PENDING_USER_FLOW_ACTION_RESPONSES =
  """
[
  {
    "jsonrpc": "2.0",
    "result": {
      "id": TX_ID,
      "sep": "24",
      "kind": "withdrawal",
      "status": "pending_user_transfer_start",
      "amount_expected": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-04T09:30:42.895791Z",
      "updated_at": "2023-08-04T09:30:43.957105200Z",
      "message": "test message 1",
      "source_account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
      "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
      "memo_type": "hash"
    },
    "id": "1"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": TX_ID,
      "sep": "24",
      "kind": "withdrawal",
      "status": "pending_anchor",
      "amount_expected": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-04T09:30:42.895791Z",
      "updated_at": "2023-08-04T09:30:45.138455Z",
      "message": "test message 2",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
          "memo_type": "hash",
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
      "source_account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
      "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
      "memo_type": "hash"
    },
    "id": "2"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": TX_ID,
      "sep": "24",
      "kind": "withdrawal",
      "status": "pending_user_transfer_complete",
      "amount_expected": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-04T09:30:42.895791Z",
      "updated_at": "2023-08-04T09:30:46.190117600Z",
      "message": "test message 3",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
          "memo_type": "hash",
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
      "source_account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
      "external_transaction_id": "ext-123456",
      "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
      "memo_type": "hash"
    },
    "id": "3"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": TX_ID,
      "sep": "24",
      "kind": "withdrawal",
      "status": "completed",
      "amount_expected": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-04T09:30:42.895791Z",
      "updated_at": "2023-08-04T09:30:46.190117600Z",
      "completed_at": "2023-08-04T09:30:46.190117600Z",
      "message": "test message 4",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
          "memo_type": "hash",
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
      "source_account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
      "external_transaction_id": "ext-123456",
      "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
      "memo_type": "hash"
    },
    "id": "4"
  }
]
"""

private const val SEP_24_WITHDRAW_COMPLETE_FULL_VIA_PENDING_USER_FLOW_ACTION_REQUESTS =
  """
[
  {
    "id": "1",
    "method": "request_onchain_funds",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 1",
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_expected": {
        "amount": "100"
      }
    }
  },
  {
    "id": "2",
    "method": "notify_onchain_funds_received",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 2",
      "stellar_transaction_id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1"
    }
  },
  {
    "id": "3",
    "method": "notify_offchain_funds_available",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 3",
      "external_transaction_id": "ext-123456"
    }
  },
  {
    "id": "4",
    "method": "notify_offchain_funds_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 4",
      "external_transaction_id": "ext-123456"
    }
  }
]
  """

private const val SEP_24_WITHDRAW_FULL_REFUND_FLOW_ACTION_REQUESTS =
  """
[
  {
    "id": "1",
    "method": "request_onchain_funds",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 1",
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_expected": {
        "amount": "100"
      }
    }
  },
  {
    "id": "2",
    "method": "notify_onchain_funds_received",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 2",
      "stellar_transaction_id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1"
    }
  },
  {
    "id": "3",
    "method": "notify_refund_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 3",
      "refund": {
        "id": "123456",
        "amount": {
          "amount": 95,
          "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        },
        "amount_fee": {
          "amount": 5,
          "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        }
      }
    }
  }
]
  """

private const val SEP_24_WITHDRAW_FULL_REFUND_FLOW_ACTION_RESPONSES =
  """
  [
  {
    "jsonrpc": "2.0",
    "result": {
      "id": TX_ID,
      "sep": "24",
      "kind": "withdrawal",
      "status": "pending_user_transfer_start",
      "amount_expected": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-04T09:30:42.895791Z",
      "updated_at": "2023-08-04T09:30:43.957105200Z",
      "message": "test message 1",
      "source_account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
      "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
      "memo_type": "hash"
    },
    "id": "1"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": TX_ID,
      "sep": "24",
      "kind": "withdrawal",
      "status": "pending_anchor",
      "amount_expected": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-04T09:30:42.895791Z",
      "updated_at": "2023-08-04T09:30:45.138455Z",
      "message": "test message 2",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
          "memo_type": "hash",
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
      "source_account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
      "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
      "memo_type": "hash"
    },
    "id": "2"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": TX_ID,
      "sep": "24",
      "kind": "withdrawal",
      "status": "refunded",
      "amount_expected": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "100",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "95",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-04T09:30:42.895791Z",
      "updated_at": "2023-08-04T09:30:46.190117600Z",
      "completed_at": "2023-08-04T09:30:46.190117600Z",
      "message": "test message 3",
      "refunds": {
        "amount_refunded": {
          "amount": "100",
          "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        },
        "amount_fee": {
          "amount": "5",
          "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        },
        "payments": [
          {
            "id": "123456",
            "id_type": "stellar",
            "amount": {
              "amount": "95",
              "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
            },
            "fee": {
              "amount": "5",
              "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
            }
          }
        ]
      },
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
          "memo_type": "hash",
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
      "source_account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
      "memo": "MTkwYjA2NTAtMDcwNy00YmMzLTk1MjEtM2ZhYzY4MzU=",
      "memo_type": "hash"
    },
    "id": "3"
  }
]
"""

private const val SEP_31_RECEIVE_REFUNDED_SHORT_FLOW_ACTION_REQUESTS =
  """
[
  {
    "id": "1",
    "method": "notify_onchain_funds_received",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 1",
      "stellar_transaction_id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1"
    }
  },
  {
    "id": "2",
    "method": "notify_refund_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 2",
      "refund": {
        "id": "123456",
        "amount": {
          "amount": "1",
          "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        },
        "amount_fee": {
          "amount": "1",
          "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        }
      }
    }
  }
]   
  """

private const val SEP_31_RECEIVE_REFUNDED_SHORT_FLOW_ACTION_RESPONSES =
  """
[
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "31",
      "kind": "receive",
      "status": "pending_receiver",
      "amount_expected": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {},
      "amount_fee": {
        "amount": "0.3",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-03T15:14:02.506800Z",
      "updated_at": "2023-08-03T15:14:05.060150Z",
      "transfer_received_at": "2023-06-22T08:46:56Z",
      "message": "test message 1",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "NTY3NWI1YzctMGVmNC00NTY2LWFmMGMtOWY4MGVmMjg=",
          "memo_type": "hash",
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
      "customers": {
        "sender": {
          "id": "SENDER_ID"
        },
        "receiver": {
          "id": "RECEIVER_ID"
        }
      },
      "creator": {
        "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
      }
    },
    "id": "1"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "31",
      "kind": "receive",
      "status": "pending_anchor",
      "amount_expected": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {},
      "amount_fee": {
        "amount": "0.3",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-03T15:14:02.506800Z",
      "updated_at": "2023-08-03T15:14:06.437196Z",
      "transfer_received_at": "2023-06-22T08:46:56Z",
      "message": "test message 2",
      "refunds": {
        "amount_refunded": {
          "amount": "2",
          "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        },
        "amount_fee": {
          "amount": "1",
          "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        },
        "payments": [
          {
            "id": "123456",
            "id_type": "stellar",
            "amount": {
              "amount": "1",
              "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
            },
            "fee": {
              "amount": "1",
              "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
            }
          }
        ]
      },
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "NTY3NWI1YzctMGVmNC00NTY2LWFmMGMtOWY4MGVmMjg=",
          "memo_type": "hash",
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
      "customers": {
        "sender": {
          "id": "SENDER_ID"
        },
        "receiver": {
          "id": "RECEIVER_ID"
        }
      },
      "creator": {
        "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
      }
    },
    "id": "2"
  }
]  
  """

private const val SEP_31_RECEIVE_COMPLETE_FULL_WITH_RECOVERY_FLOW_ACTION_REQUESTS =
  """ 
[
  {
    "id": "1",
    "method": "notify_onchain_funds_received",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 1",
      "stellar_transaction_id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1"
    }
  },
  {
    "id": "2",
    "method": "request_customer_info_update",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 2"
    }
  },
  {
    "id": "3",
    "method": "notify_customer_info_updated",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 3"
    }
  },
  {
    "id": "4",
    "method": "notify_transaction_error",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 4"
    }
  },
  {
    "id": "5",
    "method": "notify_transaction_recovery",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 5"
    }
  },
  {
    "id": "6",
    "method": "notify_offchain_funds_pending",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 6",
      "external_transaction_id": "ext123456789"
    }
  },
  {
    "id": "7",
    "method": "notify_offchain_funds_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 7",
      "external_transaction_id": "ext123456789"
    }
  }
]
  """

private const val SEP_31_RECEIVE_COMPLETE_FULL_WITH_RECOVERY_FLOW_ACTION_RESPONSES =
  """ 
[
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "31",
      "kind": "receive",
      "status": "pending_receiver",
      "amount_expected": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {},
      "amount_fee": {
        "amount": "0.3",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-07T08:01:24.478460Z",
      "updated_at": "2023-08-07T08:01:25.680087Z",
      "transfer_received_at": "2023-06-22T08:46:56Z",
      "message": "test message 1",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "ZmQzY2I4M2YtY2UwNC00Mjc2LWFiYzEtY2QzNWUzNDk=",
          "memo_type": "hash",
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
      "customers": {
        "sender": {
          "id": "SENDER_ID"
        },
        "receiver": {
          "id": "RECEIVER_ID"
        }
      },
      "creator": {
        "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
      }
    },
    "id": "1"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "31",
      "kind": "receive",
      "status": "pending_customer_info_update",
      "amount_expected": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {},
      "amount_fee": {
        "amount": "0.3",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-07T08:01:24.478460Z",
      "updated_at": "2023-08-07T08:01:26.692095Z",
      "transfer_received_at": "2023-06-22T08:46:56Z",
      "message": "test message 2",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "ZmQzY2I4M2YtY2UwNC00Mjc2LWFiYzEtY2QzNWUzNDk=",
          "memo_type": "hash",
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
      "customers": {
        "sender": {
          "id": "SENDER_ID"
        },
        "receiver": {
          "id": "RECEIVER_ID"
        }
      },
      "creator": {
        "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
      }
    },
    "id": "2"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "31",
      "kind": "receive",
      "status": "pending_receiver",
      "amount_expected": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {},
      "amount_fee": {
        "amount": "0.3",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-07T08:01:24.478460Z",
      "updated_at": "2023-08-07T08:01:27.729558Z",
      "transfer_received_at": "2023-06-22T08:46:56Z",
      "message": "test message 3",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "ZmQzY2I4M2YtY2UwNC00Mjc2LWFiYzEtY2QzNWUzNDk=",
          "memo_type": "hash",
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
      "customers": {
        "sender": {
          "id": "SENDER_ID"
        },
        "receiver": {
          "id": "RECEIVER_ID"
        }
      },
      "creator": {
        "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
      }
    },
    "id": "3"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "31",
      "kind": "receive",
      "status": "error",
      "amount_expected": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {},
      "amount_fee": {
        "amount": "0.3",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-07T08:01:24.478460Z",
      "updated_at": "2023-08-07T08:01:28.769798Z",
      "transfer_received_at": "2023-06-22T08:46:56Z",
      "message": "test message 4",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "ZmQzY2I4M2YtY2UwNC00Mjc2LWFiYzEtY2QzNWUzNDk=",
          "memo_type": "hash",
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
      "customers": {
        "sender": {
          "id": "SENDER_ID"
        },
        "receiver": {
          "id": "SENDER_ID"
        }
      },
      "creator": {
        "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
      }
    },
    "id": "4"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "31",
      "kind": "receive",
      "status": "pending_receiver",
      "amount_expected": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {},
      "amount_fee": {
        "amount": "0.3",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-07T08:01:24.478460Z",
      "updated_at": "2023-08-07T08:01:29.797331Z",
      "transfer_received_at": "2023-06-22T08:46:56Z",
      "message": "test message 5",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "ZmQzY2I4M2YtY2UwNC00Mjc2LWFiYzEtY2QzNWUzNDk=",
          "memo_type": "hash",
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
      "customers": {
        "sender": {
          "id": "SENDER_ID"
        },
        "receiver": {
          "id": "RECEIVER_ID"
        }
      },
      "creator": {
        "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
      }
    },
    "id": "5"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "31",
      "kind": "receive",
      "status": "pending_external",
      "amount_expected": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {},
      "amount_fee": {
        "amount": "0.3",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-07T08:01:24.478460Z",
      "updated_at": "2023-08-07T08:01:30.816913Z",
      "transfer_received_at": "2023-06-22T08:46:56Z",
      "message": "test message 6",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "ZmQzY2I4M2YtY2UwNC00Mjc2LWFiYzEtY2QzNWUzNDk=",
          "memo_type": "hash",
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
      "external_transaction_id": "ext123456789",
      "customers": {
        "sender": {
          "id": "SENDER_ID"
        },
        "receiver": {
          "id": "RECEIVER_ID"
        }
      },
      "creator": {
        "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
      }
    },
    "id": "6"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "31",
      "kind": "receive",
      "status": "completed",
      "amount_expected": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "10",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {},
      "amount_fee": {
        "amount": "0.3",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-07T08:01:24.478460Z",
      "updated_at": "2023-08-07T08:01:31.828811Z",
      "completed_at": "2023-08-07T08:01:31.828809Z",
      "transfer_received_at": "2023-06-22T08:46:56Z",
      "message": "test message 7",
      "stellar_transactions": [
        {
          "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
          "memo": "ZmQzY2I4M2YtY2UwNC00Mjc2LWFiYzEtY2QzNWUzNDk=",
          "memo_type": "hash",
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
      "external_transaction_id": "ext123456789",
      "customers": {
        "sender": {
          "id": "SENDER_ID"
        },
        "receiver": {
          "id": "RECEIVER_ID"
        }
      },
      "creator": {
        "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
      }
    },
    "id": "7"
  }
]
  """

private const val VALIDATIONS_AND_ERRORS_REQUESTS =
  """
[
  {
    "id": "1",
    "method": "notify_interactive_flow_completed",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_1",
      "message": "test message 1",
      "amount_in": {
        "amount": "100",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "95",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "3"
      }
    }
  },
  {
    "method": "notify_interactive_flow_completed",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 2",
      "amount_in": {
        "amount": "100",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "95",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "3"
      }
    }
  },
  {
    "id": "3",
    "method": "notify_interactive_flow_completed",
    "jsonrpc": "3.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 3",
      "amount_in": {
        "amount": "100",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "95",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "3"
      }
    }
  },
  {
    "id": "4",
    "method": "unsupported method",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 4",
      "amount_in": {
        "amount": "100",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "95",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "3"
      }
    }
  },
  {
    "id": "5",
    "method": "request_onchain_funds",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 5",
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "10.11"
      }
    }
  },
  {
    "id": "6",
    "method": "request_offchain_funds",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 6",
      "amount_in": {
        "amount": "10.11",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "9",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_expected": {
        "amount": "10.11"
      }
    }
  },
  {
    "id": "7",
    "method": "request_offchain_funds",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 7",
      "amount_in": {
        "amount": "0",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5iso4217:USD"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "10.11"
      }
    }
  },
  {
    "id": "8",
    "method": "request_offchain_funds",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 8",
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "0",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5iso4217:USD"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "10.11"
      }
    }
  },
  {
    "id": "9",
    "method": "request_offchain_funds",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 9",
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso111:III"
      },
      "amount_expected": {
        "amount": "10.11"
      }
    }
  },
  {
    "id": "10",
    "method": "notify_interactive_flow_completed",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 10",
      "amount_in": {
        "amount": "100",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "95",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "3"
      }
    }
  },
  {
    "id": "11",
    "method": "notify_interactive_flow_completed",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 11",
      "amount_in": {
        "amount": "100",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "95",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "3"
      }
    }
  },
  {
    "id": "12",
    "method": "request_offchain_funds",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 12",
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "10.11"
      }
    }
  },
  {
    "id": "13",
    "method": "request_offchain_funds",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 13",
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "amount_expected": {
        "amount": "10.11"
      }
    }
  },
  {
    "id": "14",
    "method": "notify_onchain_funds_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 14",
      "stellar_transaction_id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1"
    }
  },
  {
  "id": "15",
  "method": "notify_offchain_funds_received",
  "jsonrpc": "2.0",
  "params": {
    "transaction_id": "TX_ID",
    "message": "test message 15",
    "funds_received_at": "2023-07-04T12:34:56Z",
    "external_transaction_id": "ext-123456",
    "amount_in": {
        "amount": "10.11"
      },
      "amount_out": {
        "amount": "9"
      },
      "amount_fee": {
        "amount": "1.11"
      },
      "amount_expected": {
        "amount": "10.11"
      }
    }
  },
  {
  "id": "16",
  "method": "notify_offchain_funds_received",
  "jsonrpc": "2.0",
  "params": {
    "transaction_id": "TX_ID",
    "message": "test message 16",
    "funds_received_at": "2023-07-04T12:34:56Z",
    "external_transaction_id": "ext-123456",
    "amount_in": {
        "amount": "10.11"
      },
      "amount_out": {
        "amount": "9"
      },
      "amount_fee": {
        "amount": "1.11"
      },
      "amount_expected": {
        "amount": "10.11"
      }
    }
  },
  {
    "id": "17",
    "method": "notify_refund_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 17",
      "refund": {
        "id": "123456",
        "amount": {
          "amount": "989.11",
          "asset": "iso4217:USD"
        },
        "amount_fee": {
          "amount": "1",
          "asset": "iso4217:USD"
        }
      }
    }
  },
  {
    "id": "18",
    "method": "notify_refund_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 18",
      "refund": {
        "id": "123456",
        "amount": {
          "amount": "989.11",
          "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        },
        "amount_fee": {
          "amount": "1",
          "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        }
      }
    }
  },
  {
    "id": "19",
    "method": "notify_onchain_funds_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 19",
      "stellar_transaction_id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1"
    }
  },
  {
    "id": "20",
    "method": "notify_onchain_funds_sent",
    "jsonrpc": "2.0",
    "params": {
      "transaction_id": "TX_ID",
      "message": "test message 20",
      "stellar_transaction_id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1"
    }
  }
]
  """

private const val VALIDATIONS_AND_ERRORS_RESPONSES =
  """
[
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_1",
      "code": -32600,
      "message": "Transaction with id[TX_1] is not found"
    },
    "id": "1"
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32600,
      "message": "Id can't be NULL"
    }
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32600,
      "message": "Unsupported JSON-RPC protocol version[3.0]"
    },
    "id": "3"
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32601,
      "message": "No matching RPC method[unsupported method]"
    },
    "id": "4"
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32600,
      "message": "RPC method[request_onchain_funds] is not supported. Status[incomplete], kind[deposit], protocol[24], funds received[false]"
    },
    "id": "5"
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32602,
      "message": "amount_in.asset should be non-stellar asset"
    },
    "id": "6"
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32602,
      "message": "amount_in.amount should be positive"
    },
    "id": "7"
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32602,
      "message": "amount_out.amount should be positive"
    },
    "id": "8"
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32602,
      "message": "'iso111:III' is not a supported asset."
    },
    "id": "9"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_anchor",
      "amount_expected": {
        "amount": "3",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "100",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "95",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-07T10:53:58.811934Z",
      "updated_at": "2023-08-07T10:53:59.928900700Z",
      "message": "test message 10",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    },
    "id": "10"
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32600,
      "message": "RPC method[notify_interactive_flow_completed] is not supported. Status[pending_anchor], kind[deposit], protocol[24], funds received[false]"
    },
    "id": "11"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_user_transfer_start",
      "amount_expected": {
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-07T10:53:58.811934Z",
      "updated_at": "2023-08-07T10:54:00.955526600Z",
      "message": "test message 12",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    },
    "id": "12"
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32600,
      "message": "RPC method[request_offchain_funds] is not supported. Status[pending_user_transfer_start], kind[deposit], protocol[24], funds received[false]"
    },
    "id": "13"
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32600,
      "message": "RPC method[notify_onchain_funds_sent] is not supported. Status[pending_user_transfer_start], kind[deposit], protocol[24], funds received[false]"
    },
    "id": "14"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_anchor",
      "amount_expected": {
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-07T10:53:58.811934Z",
      "updated_at": "2023-08-07T10:54:01.996630700Z",
      "message": "test message 15",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "external_transaction_id": "ext-123456"
    },
    "id": "15"
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32600,
      "message": "RPC method[notify_offchain_funds_received] is not supported. Status[pending_anchor], kind[deposit], protocol[24], funds received[true]"
    },
    "id": "16"
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32602,
      "message": "Refund amount exceeds amount_in"
    },
    "id": "17"
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32602,
      "message": "refund.amount.asset does not match transaction amount_in_asset"
    },
    "id": "18"
  },
  {
    "jsonrpc": "2.0",
    "result": {
      "id": "TX_ID",
      "sep": "24",
      "kind": "deposit",
      "status": "completed",
      "amount_expected": {
        "amount": "10.11",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_in": {
        "amount": "10.11",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "1.11",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-07T10:53:58.811934Z",
      "updated_at": "2023-08-07T10:54:03.165199600Z",
      "completed_at": "2023-08-07T10:54:03.165199600Z",
      "message": "test message 19",
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
    "id": "19"
  },
  {
    "jsonrpc": "2.0",
    "error": {
      "id": "TX_ID",
      "code": -32600,
      "message": "RPC method[notify_onchain_funds_sent] is not supported. Status[completed], kind[deposit], protocol[24], funds received[true]"
    },
    "id": "20"
  }
]
  """

private const val SEP_24_DEPOSIT_FLOW_REQUEST = """
{
  "asset_code": "USDC"
}
  """

private const val SEP_24_WITHDRAW_FLOW_REQUEST =
  """{
    "amount": "10",
    "asset_code": "USDC",
    "asset_issuer": "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
    "account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
    "lang": "en"
}"""

private const val SEP_31_RECEIVE_FLOW_REQUEST =
  """
{
  "amount": "10",
  "asset_code": "USDC",
  "asset_issuer": "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
  "receiver_id": "RECEIVER_ID",
  "sender_id": "SENDER_ID",
  "fields": {
    "transaction": {
      "receiver_routing_number": "r0123",
      "receiver_account_number": "a0456",
      "type": "SWIFT"
    }
  }
}
  """

private const val SEP_24_DEPOSIT_REQUEST =
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

private const val CUSTOMER_1 =
  """
{
  "first_name": "John",
  "last_name": "Doe",
  "email_address": "johndoe@test.com",
  "address": "123 Washington Street",
  "city": "San Francisco",
  "state_or_province": "CA",
  "address_country_code": "US",
  "clabe_number": "1234",
  "bank_number": "abcd",
  "bank_account_number": "1234",
  "bank_account_type": "checking"
}
"""

private const val CUSTOMER_2 =
  """
{
  "first_name": "Jane",
  "last_name": "Doe",
  "email_address": "janedoe@test.com",
  "address": "321 Washington Street",
  "city": "San Francisco",
  "state_or_province": "CA",
  "address_country_code": "US",
  "clabe_number": "5678",
  "bank_number": "efgh",
  "bank_account_number": "5678",
  "bank_account_type": "checking"
}
"""
