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
import org.stellar.anchor.platform.CustodyApiClient
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.platform.gson

class CustodyApiTests(val config: TestConfig, jwt: String) {

  private val custodyApiClient = CustodyApiClient("http://localhost:8085", jwt)

  fun testAll(custodyMockServer: MockWebServer) {
    println("Performing Custody API tests...")

    `test generate deposit address`(custodyMockServer)
    `test custody transaction payment`(custodyMockServer)
  }

  private fun `test generate deposit address`(custodyMockServer: MockWebServer) {
    val response = MockResponse().setResponseCode(200).setBody(custodyDepositAddressResponse)

    custodyMockServer.enqueue(response)

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

  private fun `test custody transaction payment`(cusotodyMockServer: MockWebServer) {
    val response = MockResponse().setResponseCode(200).setBody(custodyTransactionPaymentResponse)

    cusotodyMockServer.enqueue(response)

    custodyApiClient.createTransaction(
      gson.fromJson(custodyTransactionRequest, CreateCustodyTransactionRequest::class.java)
    )

    val ex: SepException = assertThrows { custodyApiClient.createTransactionPayment("invalidId") }
    Assertions.assertInstanceOf(SepNotFoundException::class.java, ex)

    custodyApiClient.createTransactionPayment("testId")

    val recordedRequest = cusotodyMockServer.takeRequest()

    val requestUrl = recordedRequest.requestUrl
    val requestBody = recordedRequest.body.readUtf8()

    Assertions.assertEquals("${cusotodyMockServer.url("")}/v1/transactions", requestUrl.toString())
    JSONAssert.assertEquals(custodyTransactionPaymentRequest, requestBody, JSONCompareMode.STRICT)
  }
}

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
    "id" : "testId",
    "memo":  "testMemo",
    "memoType": "testMemoType",
    "protocol": "testProtocol",
    "fromAccount": "testFromAccount",
    "toAccount": "testToAccount",
    "amountIn": "testAmountIn",
    "amountInAsset": "testAmountInAsset",
    "amountOut": "50",
    "amountOutAsset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
    "kind": "testKind",
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
      "amount": "50"
    }
"""

private const val custodyTransactionPaymentResponse =
  """
    {
      "id":"testId",
      "status": "SUBMITTED"
    }
"""
