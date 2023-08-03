package org.stellar.anchor.platform.test

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.comparator.CustomComparator
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.exception.SepNotFoundException
import org.stellar.anchor.api.rpc.RpcRequest
import org.stellar.anchor.api.rpc.action.ActionMethod
import org.stellar.anchor.api.rpc.action.NotifyOffchainFundsReceivedRequest
import org.stellar.anchor.api.rpc.action.RequestOffchainFundsRequest
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.platform.CustodyApiClient
import org.stellar.anchor.platform.Sep24Client
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.platform.custody.fireblocks.FireblocksEventService.FIREBLOCKS_SIGNATURE_HEADER
import org.stellar.anchor.platform.gson
import org.stellar.anchor.platform.utils.RpcUtil.JSON_RPC_VERSION
import org.stellar.anchor.util.Sep1Helper

class CustodyApiTests(val config: TestConfig, val toml: Sep1Helper.TomlContent, jwt: String) {

  companion object {
    const val TX_ID_KEY = "TX_ID"
  }

  private val custodyApiClient = CustodyApiClient(config.env["custody.server.url"]!!, jwt)
  private val sep24Client = Sep24Client(toml.getString("TRANSFER_SERVER_SEP0024"), jwt)
  private val platformApiClient =
    PlatformApiClient(AuthHelper.forNone(), config.env["platform.server.url"]!!)

  fun testAll(custodyMockServer: MockWebServer) {
    println("Performing Custody API tests...")

    `test generate deposit address`(custodyMockServer)
    `test custody transaction payment`(custodyMockServer)
  }

  private fun `test generate deposit address`(custodyMockServer: MockWebServer) {
    val ex: SepException = assertThrows { custodyApiClient.generateDepositAddress("invalidAsset") }
    Assertions.assertEquals(
      "{\"error\":\"Unable to find Fireblocks asset code by Stellar asset code [invalidAsset]\"}",
      ex.message
    )

    val mockedCustodyDepositAddressResponse =
      MockResponse().setResponseCode(200).setBody(custodyDepositAddressResponse)

    custodyMockServer.enqueue(mockedCustodyDepositAddressResponse)

    val depositAddressResponse =
      custodyApiClient.generateDepositAddress(
        "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      )
    JSONAssert.assertEquals(
      expectedDepositAddress,
      gson.toJson(depositAddressResponse),
      JSONCompareMode.STRICT
    )

    val recordedRequest = custodyMockServer.takeRequest()

    val requestUrl = recordedRequest.requestUrl
    val requestBody = recordedRequest.body.readUtf8()

    Assertions.assertEquals(
      "${custodyMockServer.url("")}/v1/vault/accounts/1/XLM_USDC_T_CEKS/addresses",
      requestUrl.toString()
    )
    JSONAssert.assertEquals(custodyDepositAddressRequest, requestBody, JSONCompareMode.STRICT)
  }

  private fun `test custody transaction payment`(custodyMockServer: MockWebServer) {
    val depositRequest = gson.fromJson(depositRequest, HashMap::class.java)
    val depositResponse = sep24Client.deposit(depositRequest as HashMap<String, String>)
    val txId = depositResponse.id

    val mockedCustodyTransactionPaymentResponse =
      MockResponse().setResponseCode(200).setBody(custodyTransactionPaymentResponse)

    custodyMockServer.enqueue(mockedCustodyTransactionPaymentResponse)

    custodyApiClient.createTransaction(
      gson.fromJson(
        custodyTransactionRequest.replace(TX_ID_KEY, txId),
        CreateCustodyTransactionRequest::class.java
      )
    )

    val requestOffchainFundsParams =
      gson.fromJson(REQUEST_OFFCHAIN_FUNDS, RequestOffchainFundsRequest::class.java)
    requestOffchainFundsParams.transactionId = txId
    var rpcRequest =
      RpcRequest.builder()
        .method(ActionMethod.REQUEST_OFFCHAIN_FUNDS.toString())
        .jsonrpc(JSON_RPC_VERSION)
        .params(requestOffchainFundsParams)
        .id(1)
        .build()
    platformApiClient.callRpcAction(listOf(rpcRequest))

    var txResponse = platformApiClient.getTransaction(txId)
    Assertions.assertEquals(SepTransactionStatus.PENDING_USR_TRANSFER_START, txResponse.status)

    val notifyOffchainFundsReceivedParams =
      gson.fromJson(NOTIFY_OFFCHAIN_FUNDS_RECEIVED, NotifyOffchainFundsReceivedRequest::class.java)
    notifyOffchainFundsReceivedParams.transactionId = txId
    rpcRequest =
      RpcRequest.builder()
        .id(2)
        .method(ActionMethod.NOTIFY_OFFCHAIN_FUNDS_RECEIVED.toString())
        .jsonrpc(JSON_RPC_VERSION)
        .params(notifyOffchainFundsReceivedParams)
        .build()
    platformApiClient.callRpcAction(listOf(rpcRequest))

    txResponse = platformApiClient.getTransaction(txId)
    Assertions.assertEquals(SepTransactionStatus.PENDING_ANCHOR, txResponse.status)

    val ex: SepException = assertThrows { custodyApiClient.createTransactionPayment("invalidId") }
    Assertions.assertInstanceOf(SepNotFoundException::class.java, ex)

    custodyApiClient.createTransactionPayment(txId)

    val recordedRequest = custodyMockServer.takeRequest()

    val requestUrl = recordedRequest.requestUrl
    val requestBody = recordedRequest.body.readUtf8()

    Assertions.assertEquals("${custodyMockServer.url("")}/v1/transactions", requestUrl.toString())
    JSONAssert.assertEquals(custodyTransactionPaymentRequest, requestBody, JSONCompareMode.STRICT)

    custodyApiClient.sendWebhook(
      webhookRequest,
      mapOf(FIREBLOCKS_SIGNATURE_HEADER to webhookSignature)
    )

    txResponse = platformApiClient.getTransaction(txId)
    txResponse.startedAt = null
    txResponse.updatedAt = null

    JSONAssert.assertEquals(
      expectedTransactionResponse.replace(TX_ID_KEY, txId),
      gson.toJson(txResponse),
      CustomComparator(
        JSONCompareMode.STRICT,
        Customization("completed_at") { _, _ -> true },
        Customization("stellar_transactions[0].created_at") { _, _ -> true }
      )
    )
  }
}

private const val depositRequest =
  """{
    "asset_code": "USDC",
    "asset_issuer": "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
    "lang": "en"
}"""

private const val expectedDepositAddress =
  """
  {
    "memoType":"id",
    "memo": "testTag",
    "address": "testAddress"
  }
"""

private const val custodyDepositAddressRequest = """
  {
  }
"""

private const val custodyDepositAddressResponse =
  """
  {
    "address":"testAddress",
    "legacyAddress": "testLegacyAddress",
    "enterpriseAddress": "testEnterpriseAddress",
    "tag":"testTag",
    "bip44AddressIndex":12345
  }
"""

private const val custodyTransactionRequest =
  """
  {
    "id" : "TX_ID",
    "memo":  "testMemo",
    "memoType": "testMemoType",
    "protocol": "24",
    "fromAccount": "testFromAccount",
    "toAccount": "testToAccount",
    "amount": "0.45",
    "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
    "kind": "deposit",
    "requestAssetCode": "testRequestAssetCode",
    "requestAssetIssuer": "testRequestAssetIssuer"
  }
"""

private const val custodyTransactionPaymentRequest =
  """
    {
      "assetId": "XLM_USDC_T_CEKS",
      "source": {
        "type": "VAULT_ACCOUNT",
        "id": "1"
      },
      "destination": {
        "type": "ONE_TIME_ADDRESS",
        "oneTimeAddress": {
          "address": "testToAccount",
          "tag": "testMemo"
        }
      },
      "amount": "0.45"
    }
"""

private const val custodyTransactionPaymentResponse =
  """
    {
      "id":"df0442b4-6d53-44cd-82d7-3c48edc0b1ac",
      "status": "SUBMITTED"
    }
"""

private const val webhookRequest =
  """
  {
  "type": "TRANSACTION_STATUS_UPDATED",
  "tenantId": "6ae8e895-7bdb-5021-b865-c65885c61068",
  "timestamp": 1687423621679,
  "data": {
    "id": "df0442b4-6d53-44cd-82d7-3c48edc0b1ac",
    "createdAt": 1687423599336,
    "lastUpdated": 1687423620808,
    "assetId": "XLM_USDC_T_CEKS",
    "source": {
      "id": "4",
      "type": "VAULT_ACCOUNT",
      "name": "NewVault",
      "subType": ""
    },
    "destination": {
      "id": null,
      "type": "ONE_TIME_ADDRESS",
      "name": "N/A",
      "subType": ""
    },
    "amount": 4.5,
    "networkFee": 0.00001,
    "netAmount": 4.5,
    "sourceAddress": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6",
    "destinationAddress": "GC64QXQEVU33BHZGWGC3K637HACYTDWA6JHPDN5NQIRBMDC637UL4F2W",
    "destinationAddressDescription": "",
    "destinationTag": "",
    "status": "CONFIRMING",
    "txHash": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
    "subStatus": "CONFIRMED",
    "signedBy": [],
    "createdBy": "1444ed36-5bc0-4e3b-9b17-5df29fc0590f",
    "rejectedBy": "",
    "amountUSD": 4.509,
    "addressType": "",
    "note": "",
    "exchangeTxId": "",
    "requestedAmount": 4.5,
    "feeCurrency": "XLM_TEST",
    "operation": "TRANSFER",
    "customerRefId": null,
    "numOfConfirmations": 1,
    "amountInfo": {
      "amount": "4.5",
      "requestedAmount": "4.5",
      "netAmount": "4.5",
      "amountUSD": "4.509"
    },
    "feeInfo": {
      "networkFee": "0.00001"
    },
    "destinations": [],
    "externalTxId": null,
    "blockInfo": {
      "blockHeight": "131155",
      "blockHash": "46185b809c09e29da09bdc667c947b23fb2716ed8735227c0e77c19fdb620fd4"
    },
    "signedMessages": [],
    "assetType": "XLM_ASSET"
  }
}
"""

private const val webhookSignature =
  "jwWIW/EX4XdkD9sS0YSybaYCnITwdDsCADV99mVyimhLPz6EhQDV6hJEfA4/BcNtXveJNbchKCwVI1l5o0eHc/1F0l4WsfIGNcDl68CDBWpe6LyQ3ZWUS7X/VMEeFFTBkgGcRl7aDjX2Yn9HuLFnSFRR2r4eDKP8y4G7hUbPUdE="

private const val expectedTransactionResponse =
  """
  {
  "id": "TX_ID",
  "sep": "24",
  "kind": "deposit",
  "status": "completed",
  "amount_in": {
    "amount": "1",
    "asset": "iso4217:USD"
  },
  "message": "Outgoing payment sent",
  "amount_out": {
    "amount": "0.9",
    "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
  },
  "amount_fee": {
    "amount": "0.1",
    "asset": "iso4217:USD"
  },
  "amount_expected": {
    "amount": "1",
    "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
  },
  "completed_at": "2023-06-22T08:46:39.336Z",
  "external_transaction_id": "1",
  "stellar_transactions": [
    {
      "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
      "created_at": "2023-06-22T08:46:39.336Z",
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
  "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
}
"""

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
        "amount": "1",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    },
    "amount_out": {
        "amount": "0.9",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    },
    "amount_fee": {
        "amount": "0.1",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    },
    "external_transaction_id": "1"
  }"""
