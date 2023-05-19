package org.stellar.anchor.platform.test

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.exception.SepNotFoundException
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.platform.CustodyApiClient
import org.stellar.anchor.platform.Sep24Client
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.platform.gson
import org.stellar.anchor.util.Sep1Helper

class CustodyApiTests(val config: TestConfig, val toml: Sep1Helper.TomlContent, jwt: String) {

  companion object {
    const val TX_ID_KEY = "TX_ID"
  }

  private val custodyApiClient = CustodyApiClient("http://localhost:8085", jwt)
  private val sep24Client = Sep24Client(toml.getString("TRANSFER_SERVER_SEP0024"), jwt)
  private val platformApiClient =
    PlatformApiClient(AuthHelper.forNone(), config.env["platform.server.url"]!!)

  fun testAll(custodyMockServer: MockWebServer) {
    println("Performing Custody API tests...")

    `test generate deposit address`(custodyMockServer)
    `test custody transaction payment`(custodyMockServer)
  }

  private fun `test generate deposit address`(custodyMockServer: MockWebServer) {
    val mockedCustodyDepositAddressResponse =
      MockResponse().setResponseCode(200).setBody(custodyDepositAddressResponse)

    custodyMockServer.enqueue(mockedCustodyDepositAddressResponse)

    val ex: SepException = assertThrows { custodyApiClient.generateDepositAddress("invalidAsset") }
    Assertions.assertEquals(
      "{\"error\":\"Unable to find Fireblocks asset code by Stellar asset code [invalidAsset]\"}",
      ex.message
    )

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

    val ex: SepException = assertThrows { custodyApiClient.createTransactionPayment("invalidId") }
    Assertions.assertInstanceOf(SepNotFoundException::class.java, ex)

    custodyApiClient.createTransactionPayment(txId)

    val recordedRequest = custodyMockServer.takeRequest()

    val requestUrl = recordedRequest.requestUrl
    val requestBody = recordedRequest.body.readUtf8()

    Assertions.assertEquals("${custodyMockServer.url("")}/v1/transactions", requestUrl.toString())
    JSONAssert.assertEquals(custodyTransactionPaymentRequest, requestBody, JSONCompareMode.STRICT)

    custodyApiClient.sendWebhook(webhookRequest)

    val txResponse = platformApiClient.getTransaction(txId)
    txResponse.startedAt = null
    txResponse.updatedAt = null

    JSONAssert.assertEquals(
      expectedTransactionResponse.replace(TX_ID_KEY, txId),
      gson.toJson(txResponse),
      JSONCompareMode.STRICT
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
    "amountIn": "0.5",
    "amountInAsset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
    "amountOut": "0.5",
    "amountOutAsset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
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
          "address": "testToAccount"
        }
      },
      "amount": "0.5"
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
  "timestamp": 1684499146663,
  "data": {
    "id": "df0442b4-6d53-44cd-82d7-3c48edc0b1ac",
    "createdAt": 1684499124219,
    "lastUpdated": 1684499136416,
    "assetId": "XLM_USDC_T_CEKS",
    "source": {
      "id": "1",
      "type": "VAULT_ACCOUNT",
      "name": "TestAnchor",
      "subType": ""
    },
    "destination": {
      "id": null,
      "type": "ONE_TIME_ADDRESS",
      "name": "N/A",
      "subType": ""
    },
    "amount": 0.45,
    "networkFee": 0.001,
    "netAmount": 0.45,
    "sourceAddress": "GB4LDT5G6GC26QUDS3YQ3XCIRSIYTIIWA3PLKFP4B7PRZC5DRZKSZHSK",
    "destinationAddress": "GAA4N7CEZ4XWF5ADWRU2YWF4ZGQMT5PL4GLCZVW2UAX4IPE3ALQRO7GX",
    "destinationAddressDescription": "",
    "destinationTag": "",
    "status": "COMPLETED",
    "txHash": "dff8279e0994ff7a2c78a0d7b7cc1175b39f2ab1f45de1e02b639a85e9801f96",
    "subStatus": "CONFIRMED",
    "signedBy": [],
    "createdBy": "1444ed36-5bc0-4e3b-9b17-5df29fc0590f",
    "rejectedBy": "",
    "amountUSD": 0.44912565,
    "addressType": "",
    "note": "",
    "exchangeTxId": "",
    "requestedAmount": 0.45,
    "feeCurrency": "XLM_TEST",
    "operation": "TRANSFER",
    "customerRefId": null,
    "numOfConfirmations": 1,
    "amountInfo": {
      "amount": "0.45",
      "requestedAmount": "0.45",
      "netAmount": "0.45",
      "amountUSD": "0.44912565"
    },
    "feeInfo": {
      "networkFee": "0.001"
    },
    "destinations": [],
    "externalTxId": null,
    "blockInfo": {
      "blockHeight": "1070795",
      "blockHash": "bebee26b002678521ea47c42a23aaa35b089d0412ed20232aaf8072242c2e018"
    },
    "signedMessages": []
  }
}
"""

private const val expectedTransactionResponse =
  """
  {
  "id": "TX_ID",
  "sep": "24",
  "kind": "deposit",
  "status": "completed",
  "amount_expected": {
    "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
  },
  "transfer_received_at": "2023-05-19T12:25:24.219Z",
  "stellar_transactions": [
    {
      "id": "dff8279e0994ff7a2c78a0d7b7cc1175b39f2ab1f45de1e02b639a85e9801f96",
      "memo": "testMemo",
      "memo_type": "testMemoType",
      "created_at": "2023-05-19T12:25:24.219Z",
      "envelope": "AAAAAgAAAAB4sc+m8YWvQoOW8Q3cSIyRiaEWBt61FfwP3xyLo45VLAAPQkAADE5GAAAHDQAAAAEAAAAANTSFeAAAAABkZ5TjAAAAAAAAAAEAAAAAAAAAAQAAAAABxvxEzy9i9AO0aaxYvMmgyfXr4ZYs1tqgL8Q8mwLhFwAAAAFVU0RDAAAAAEI+fQXy7K+/7BkrIVo/G+lq7bjY5wJUq+NBPgIH3layAAAAAABEqiAAAAAAAAAAAaOOVSwAAABA1XvNJlEErMYTCK7fmi643pRZEAHqkQATR7JM+HOUA8XkzAVAwETRqhQAC/9EgzCe5XfKoiiH1o5YyJ/+4NwlDg==",
      "payments": [
        {
          "id": "4599029505736705",
          "amount": {
            "amount": "0.4500000",
            "asset": "USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "payment_type": "payment",
          "source_account": "GB4LDT5G6GC26QUDS3YQ3XCIRSIYTIIWA3PLKFP4B7PRZC5DRZKSZHSK",
          "destination_account": "GAA4N7CEZ4XWF5ADWRU2YWF4ZGQMT5PL4GLCZVW2UAX4IPE3ALQRO7GX"
        }
      ]
    }
  ],
  "destination_account": "GCHU3RZAECOKGM2YAJLQIIYB2ZPLMFTTGN5D3XZNX4RDOEERVLXO7HU4"
}  
"""
