package org.stellar.anchor.platform.test

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.comparator.CustomComparator
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.exception.SepNotFoundException
import org.stellar.anchor.api.rpc.method.*
import org.stellar.anchor.api.rpc.method.RpcMethod.*
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.platform.CustodyApiClient
import org.stellar.anchor.platform.Sep24Client
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.platform.custody.fireblocks.FireblocksEventService.FIREBLOCKS_SIGNATURE_HEADER
import org.stellar.anchor.platform.gson
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

    val mockServerDispatcher: Dispatcher =
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          if (
            "POST" == request.method &&
              "//v1/vault/accounts/1/XLM_USDC_T_CEKS/addresses" == request.path
          ) {
            return MockResponse().setResponseCode(200).setBody(CUSTODY_DEPOSIT_ADDRESS_RESPONSE)
          }
          if ("POST" == request.method && "//v1/transactions" == request.path) {
            return MockResponse().setResponseCode(200).setBody(CUSTODY_TRANSACTION_PAYMENT_RESPONSE)
          }
          return MockResponse().setResponseCode(404)
        }
      }

    custodyMockServer.dispatcher = mockServerDispatcher

    `test generate deposit address`(custodyMockServer)
    `test custody transaction payment`(custodyMockServer)
    `test custody transaction refund`(custodyMockServer)
  }

  private fun `test generate deposit address`(custodyMockServer: MockWebServer) {
    val ex: SepException = assertThrows { custodyApiClient.generateDepositAddress("invalidAsset") }
    assertEquals(
      "{\"error\":\"Unable to find Fireblocks asset code by Stellar asset code [invalidAsset]\"}",
      ex.message
    )

    val depositAddressResponse =
      custodyApiClient.generateDepositAddress(
        "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      )
    JSONAssert.assertEquals(
      EXPECTED_DEPOSIT_ADDRESS,
      gson.toJson(depositAddressResponse),
      JSONCompareMode.STRICT
    )

    val recordedRequest = custodyMockServer.takeRequest()

    val requestPath = recordedRequest.path
    val requestMethod = recordedRequest.method
    val requestBody = recordedRequest.body.readUtf8()

    assertEquals("//v1/vault/accounts/1/XLM_USDC_T_CEKS/addresses", requestPath.toString())
    assertEquals("POST", requestMethod)
    JSONAssert.assertEquals(CUSTODY_DEPOSIT_ADDRESS_REQUEST, requestBody, JSONCompareMode.STRICT)
  }

  private fun `test custody transaction payment`(custodyMockServer: MockWebServer) {
    val depositRequest = gson.fromJson(DEPOSIT_REQUEST, HashMap::class.java)

    @Suppress("UNCHECKED_CAST")
    val depositResponse = sep24Client.deposit(depositRequest as HashMap<String, String>)
    val txId = depositResponse.id

    custodyApiClient.createTransaction(
      gson.fromJson(
        CUSTODY_TRANSACTION_REQUEST.replace(TX_ID_KEY, txId),
        CreateCustodyTransactionRequest::class.java
      )
    )

    val requestOffchainFundsParams =
      gson.fromJson(REQUEST_OFFCHAIN_FUNDS_REQUEST, RequestOffchainFundsRequest::class.java)
    requestOffchainFundsParams.transactionId = txId
    platformApiClient.sendRpcNotification(REQUEST_OFFCHAIN_FUNDS, requestOffchainFundsParams)

    var txResponse = platformApiClient.getTransaction(txId)
    assertEquals(SepTransactionStatus.PENDING_USR_TRANSFER_START, txResponse.status)

    val notifyOffchainFundsReceivedParams =
      gson.fromJson(
        NOTIFY_OFFCHAIN_FUNDS_RECEIVED_REQUEST,
        NotifyOffchainFundsReceivedRequest::class.java
      )
    notifyOffchainFundsReceivedParams.transactionId = txId
    platformApiClient.sendRpcNotification(
      NOTIFY_OFFCHAIN_FUNDS_RECEIVED,
      notifyOffchainFundsReceivedParams
    )

    txResponse = platformApiClient.getTransaction(txId)
    assertEquals(SepTransactionStatus.PENDING_ANCHOR, txResponse.status)

    val ex: SepException = assertThrows { custodyApiClient.createTransactionPayment("invalidId") }
    assertInstanceOf(SepNotFoundException::class.java, ex)

    custodyApiClient.createTransactionPayment(txId)

    val recordedRequest = custodyMockServer.takeRequest()

    val requestPath = recordedRequest.path
    val requestMethod = recordedRequest.method
    val requestBody = recordedRequest.body.readUtf8()

    assertEquals("//v1/transactions", requestPath.toString())
    JSONAssert.assertEquals(
      CUSTODY_TRANSACTION_PAYMENT_REQUEST,
      requestBody,
      JSONCompareMode.STRICT
    )

    assertEquals("POST", requestMethod)

    custodyApiClient.sendWebhook(
      WEBHOOK_REQUEST,
      mapOf(FIREBLOCKS_SIGNATURE_HEADER to WEBHOOK_SIGNATURE)
    )

    txResponse = platformApiClient.getTransaction(txId)
    txResponse.startedAt = null
    txResponse.updatedAt = null

    JSONAssert.assertEquals(
      EXPECTED_TRANSACTION_RESPONSE.replace(TX_ID_KEY, txId),
      gson.toJson(txResponse),
      CustomComparator(
        JSONCompareMode.STRICT,
        Customization("completed_at") { _, _ -> true },
        Customization("stellar_transactions[0].created_at") { _, _ -> true }
      )
    )
  }

  private fun `test custody transaction refund`(custodyMockServer: MockWebServer) {
    val withdrawRequest = gson.fromJson(SEP_24_WITHDRAW_FLOW_REQUEST, HashMap::class.java)

    @Suppress("UNCHECKED_CAST")
    val withdrawResponse = sep24Client.withdraw(withdrawRequest as HashMap<String, String>)
    val txId = withdrawResponse.id

    val mockServerDispatcher: Dispatcher =
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          if (
            "POST" == request.method &&
              "//v1/vault/accounts/1/XLM_USDC_T_CEKS/addresses" == request.path
          ) {
            return MockResponse().setResponseCode(200).setBody(CUSTODY_DEPOSIT_ADDRESS_RESPONSE)
          }
          if ("POST" == request.method && "//v1/transactions" == request.path) {
            return MockResponse().setResponseCode(200).setBody(CUSTODY_TRANSACTION_REFUND_RESPONSE)
          }
          return MockResponse().setResponseCode(404)
        }
      }

    custodyMockServer.dispatcher = mockServerDispatcher

    val requestOnchainFundsParams =
      gson.fromJson(
        REQUEST_ONCHAIN_FUNDS_REQUEST.replace(TX_ID_KEY, txId),
        RequestOnchainFundsRequest::class.java
      )
    platformApiClient.sendRpcNotification(REQUEST_ONCHAIN_FUNDS, requestOnchainFundsParams)

    var txResponse = platformApiClient.getTransaction(txId)
    assertEquals(SepTransactionStatus.PENDING_USR_TRANSFER_START, txResponse.status)

    val notifyOnchainFundsReceivedRequest =
      gson.fromJson(
        NOTIFY_ONCHAIN_FUNDS_RECEIVED_REQUEST.replace(TX_ID_KEY, txId),
        NotifyOnchainFundsReceivedRequest::class.java
      )
    platformApiClient.sendRpcNotification(
      NOTIFY_ONCHAIN_FUNDS_RECEIVED,
      notifyOnchainFundsReceivedRequest
    )

    txResponse = platformApiClient.getTransaction(txId)
    assertEquals(SepTransactionStatus.PENDING_ANCHOR, txResponse.status)

    val doStellarRefundParams =
      gson.fromJson(
        DO_STELLAR_REFUND_REQUEST.replace(TX_ID_KEY, txId),
        DoStellarRefundRequest::class.java
      )
    platformApiClient.sendRpcNotification(DO_STELLAR_REFUND, doStellarRefundParams)
    txResponse = platformApiClient.getTransaction(txId)
    assertEquals(SepTransactionStatus.PENDING_STELLAR, txResponse.status)

    custodyApiClient.sendWebhook(
      REFUND_WEBHOOK_REQUEST,
      mapOf(FIREBLOCKS_SIGNATURE_HEADER to REFUND_WEBHOOK_SIGNATURE)
    )

    txResponse = platformApiClient.getTransaction(txId)
    txResponse.startedAt = null
    txResponse.updatedAt = null

    JSONAssert.assertEquals(
      EXPECTED_TXN_REFUND_RESPONSE.replace(TX_ID_KEY, txId),
      gson.toJson(txResponse),
      CustomComparator(
        JSONCompareMode.STRICT,
        Customization("completed_at") { _, _ -> true },
        Customization("stellar_transactions[0].created_at") { _, _ -> true }
      )
    )
  }
}

private const val DEPOSIT_REQUEST =
  """{
    "asset_code": "USDC",
    "asset_issuer": "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
    "lang": "en"
}"""

private const val SEP_24_WITHDRAW_FLOW_REQUEST =
  """{
    "amount": "10",
    "asset_code": "USDC",
    "asset_issuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
    "account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
    "lang": "en",
    "refund_memo":  "12345",
    "refund_memo_type": "id"
}"""

private const val EXPECTED_DEPOSIT_ADDRESS =
  """
  {
    "memoType":"id",
    "memo": "testTag",
    "address": "testAddress"
  }
"""

private const val CUSTODY_DEPOSIT_ADDRESS_REQUEST = """
  {
  }
"""

private const val CUSTODY_DEPOSIT_ADDRESS_RESPONSE =
  """
  {
    "address":"testAddress",
    "legacyAddress": "testLegacyAddress",
    "enterpriseAddress": "testEnterpriseAddress",
    "tag":"testTag",
    "bip44AddressIndex":12345
  }
"""

private const val CUSTODY_TRANSACTION_REQUEST =
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

private const val CUSTODY_TRANSACTION_PAYMENT_REQUEST =
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

private const val CUSTODY_TRANSACTION_PAYMENT_RESPONSE =
  """
    {
      "id":"df0442b4-6d53-44cd-82d7-3c48edc0b1ac",
      "status": "SUBMITTED"
    }
"""

private const val CUSTODY_TRANSACTION_REFUND_RESPONSE =
  """
    {
      "id":"df0442b4-6d53-44cd-82d7-3c48edc0b1ad",
      "status": "SUBMITTED"
    }
"""

private const val WEBHOOK_REQUEST =
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

private const val REFUND_WEBHOOK_REQUEST =
  """
  {
  "type": "TRANSACTION_STATUS_UPDATED",
  "tenantId": "6ae8e895-7bdb-5021-b865-c65885c61068",
  "timestamp": 1687423621679,
  "data": {
    "id": "df0442b4-6d53-44cd-82d7-3c48edc0b1ad",
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
    "netAmount": 1,
    "sourceAddress": "GC64QXQEVU33BHZGWGC3K637HACYTDWA6JHPDN5NQIRBMDC637UL4F2W",
    "destinationAddress": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
    "destinationAddressDescription": "",
    "destinationTag": "12345",
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

private const val WEBHOOK_SIGNATURE =
  "jwWIW/EX4XdkD9sS0YSybaYCnITwdDsCADV99mVyimhLPz6EhQDV6hJEfA4/BcNtXveJNbchKCwVI1l5o0eHc/1F0l4WsfIGNcDl68CDBWpe6LyQ3ZWUS7X/VMEeFFTBkgGcRl7aDjX2Yn9HuLFnSFRR2r4eDKP8y4G7hUbPUdE="

private const val REFUND_WEBHOOK_SIGNATURE =
  "JQD32Ux2N1pJo61giuvABGyRtkn9Da1nJd8GGoedTKfvdwGFBkU4H78u0s7iHu4dhGgSg5NFL1mkkhSdoeuzkva/jgadFrn3JDQ3t1lFffiBBMNumOcn5c7ImCecIjizhR1uzW4rWmelpeG+Dah5C8q+EQ82qjlmMOVoYYuSzvM="

private const val EXPECTED_TRANSACTION_RESPONSE =
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

private const val EXPECTED_TXN_REFUND_RESPONSE =
  """
  {
  "id": "TX_ID",
  "sep": "24",
  "kind": "withdrawal",
  "status": "refunded",
  "amount_expected": {
    "amount": "4.5",
    "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
  },
  "amount_in": {
    "amount": "4.5",
    "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
  },
  "amount_out": {
    "amount": "4",
    "asset": "iso4217:USD"
  },
  "amount_fee": {
    "amount": "0.5",
    "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
  },
  "completed_at": "2023-08-08T14:07:28.799297Z",
  "message": "test message",
  "refunds": {
    "amount_refunded": {
      "amount": "4.5",
      "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    },
    "amount_fee": {
      "amount": "0",
      "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    },
    "payments": [
      {
        "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
        "id_type": "stellar",
        "amount": {
          "amount": "4.5000000",
          "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        },
        "fee": {
          "amount": "0",
          "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        }
      }
    ]
  },
  "stellar_transactions": [
    {
      "id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1",
      "memo": "testTag",
      "memo_type": "id",
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
  "destination_account": "testAddress",
  "memo": "testTag",
  "memo_type": "id"
}
"""

private const val REQUEST_OFFCHAIN_FUNDS_REQUEST =
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

private const val NOTIFY_OFFCHAIN_FUNDS_RECEIVED_REQUEST =
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

private const val REQUEST_ONCHAIN_FUNDS_REQUEST =
  """
  {
    "transaction_id": "TX_ID",
    "message": "test message 1",
    "amount_in": {
      "amount": "4.5",
      "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    },
    "amount_out": {
      "amount": "4",
      "asset": "iso4217:USD"
    },
    "amount_fee": {
      "amount": "0.5",
      "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    },
    "amount_expected": {
      "amount": "4.5"
    }
  }
"""

private const val NOTIFY_ONCHAIN_FUNDS_RECEIVED_REQUEST =
  """
  {
    "transaction_id": "TX_ID",
    "message": "test message 1",
    "stellar_transaction_id": "fba01f815acfe1f493271017f02929e97e30656ba57a5ac8f3d1356dd4926ea1"
  }
"""

private const val DO_STELLAR_REFUND_REQUEST =
  """
  {
    "transaction_id": "TX_ID",
    "message": "test message",
    "refund": {
        "amount": {
            "amount": 4.5,
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        },
        "amount_fee": {
            "amount": 0,
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
        }
    }
  }
"""
