package org.stellar.anchor.platform.test

import com.google.gson.reflect.TypeToken
import io.ktor.client.plugins.*
import io.ktor.http.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.skyscreamer.jsonassert.JSONAssert
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.api.sep.sep31.Sep31GetTransactionResponse
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.platform.*
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Log.info
import org.stellar.anchor.util.MemoHelper
import org.stellar.anchor.util.Sep1Helper
import org.stellar.anchor.util.StringHelper.json
import org.stellar.reference.client.AnchorReferenceServerClient
import org.stellar.reference.wallet.WalletServerClient
import org.stellar.walletsdk.ApplicationConfiguration
import org.stellar.walletsdk.StellarConfiguration
import org.stellar.walletsdk.Wallet
import org.stellar.walletsdk.anchor.MemoType
import org.stellar.walletsdk.asset.IssuedAssetId
import org.stellar.walletsdk.asset.StellarAssetId
import org.stellar.walletsdk.horizon.SigningKeyPair
import org.stellar.walletsdk.horizon.sign

class Sep31CustodyRpcEnd2EndTests(
  config: TestConfig,
  val toml: Sep1Helper.TomlContent,
  val jwt: String
) {
  private val gson = GsonUtils.getInstance()
  private val walletSecretKey = System.getenv("WALLET_SECRET_KEY") ?: CLIENT_WALLET_SECRET
  private val keypair = SigningKeyPair.fromSecret(walletSecretKey)
  private val wallet =
    Wallet(
      StellarConfiguration.Testnet,
      ApplicationConfiguration { defaultRequest { url { protocol = URLProtocol.HTTP } } }
    )
  private val maxTries = 90
  private val anchorReferenceServerClient =
    AnchorReferenceServerClient(Url(config.env["reference.server.url"]!!))
  private val walletServerClient = WalletServerClient(Url(config.env["wallet.server.url"]!!))
  private val sep12Client = Sep12Client(toml.getString("KYC_SERVER"), jwt)
  private val sep31Client = Sep31Client(toml.getString("DIRECT_PAYMENT_SERVER"), jwt)
  private val sep38Client = Sep38Client(toml.getString("ANCHOR_QUOTE_SERVER"), jwt)
  private val platformApiClient =
    PlatformApiClient(AuthHelper.forNone(), config.env["platform.server.url"]!!)

  private fun compareAndAssertEvents(
    asset: StellarAssetId,
    expectedEvents: List<AnchorEvent>,
    actualEvents: List<AnchorEvent>
  ) {
    expectedEvents.forEachIndexed { index, expectedEvent ->
      actualEvents[index].let { actualEvent ->
        expectedEvent.id = actualEvent.id
        expectedEvent.transaction.id = actualEvent.transaction.id
        expectedEvent.transaction.startedAt = actualEvent.transaction.startedAt
        expectedEvent.transaction.updatedAt = actualEvent.transaction.updatedAt
        expectedEvent.transaction.transferReceivedAt = actualEvent.transaction.transferReceivedAt
        expectedEvent.transaction.completedAt = actualEvent.transaction.completedAt
        expectedEvent.transaction.stellarTransactions = actualEvent.transaction.stellarTransactions
        expectedEvent.transaction.memo = actualEvent.transaction.memo
        expectedEvent.transaction.destinationAccount = actualEvent.transaction.destinationAccount
        expectedEvent.transaction.customers = actualEvent.transaction.customers
        expectedEvent.transaction.quoteId = actualEvent.transaction.quoteId
        actualEvent.transaction.amountIn?.let {
          expectedEvent.transaction.amountIn.amount = actualEvent.transaction.amountIn.amount
          expectedEvent.transaction.amountIn.asset = asset.sep38
          //          expectedEvent.transaction.amountIn.asset =
          // actualEvent.transaction.amountIn.asset
        }
        actualEvent.transaction.amountOut?.let {
          expectedEvent.transaction.amountOut.amount = actualEvent.transaction.amountOut.amount
          expectedEvent.transaction.amountOut.asset = FIAT_USD
          //          expectedEvent.transaction.amountOut.asset =
          // actualEvent.transaction.amountOut.asset
        }
        actualEvent.transaction.amountFee?.let {
          expectedEvent.transaction.amountFee.amount = actualEvent.transaction.amountFee.amount
          expectedEvent.transaction.amountFee.asset = asset.sep38
          //          expectedEvent.transaction.amountFee.asset =
          // actualEvent.transaction.amountFee.asset
        }
        actualEvent.transaction.amountExpected?.let {
          expectedEvent.transaction.amountExpected.amount =
            actualEvent.transaction.amountExpected.amount
          expectedEvent.transaction.amountExpected.asset = asset.sep38
          //          expectedEvent.transaction.amountExpected.asset =
          actualEvent.transaction.amountExpected.asset
        }
      }
    }
    JSONAssert.assertEquals(json(expectedEvents), gson.toJson(actualEvents), true)
  }

  private fun compareAndAssertCallbacks(
    asset: StellarAssetId,
    expectedCallbacks: List<Sep31GetTransactionResponse>,
    actualCallbacks: List<Sep31GetTransactionResponse>
  ) {
    expectedCallbacks.forEachIndexed { index, expectedCallback ->
      actualCallbacks[index].let { actualCallback ->
        with(expectedCallback.transaction) {
          id = actualCallback.transaction.id
          startedAt = actualCallback.transaction.startedAt
          amountIn = actualCallback.transaction.amountIn
          amountInAsset?.let { amountInAsset = asset.sep38 }
          amountOut = actualCallback.transaction.amountOut
          amountOutAsset?.let { amountOutAsset = asset.sep38 }
          amountFee = actualCallback.transaction.amountFee
          amountFeeAsset?.let { amountFeeAsset = asset.sep38 }
          stellarTransactionId = actualCallback.transaction.stellarTransactionId
        }
      }
    }
    info("callback ---------------------------------------------")
    info(json(actualCallbacks))
    JSONAssert.assertEquals(json(expectedCallbacks), json(actualCallbacks), true)
  }

  private fun `test typical receive end-to-end flow`(asset: StellarAssetId, amount: String) =
    runBlocking {
      val senderCustomerRequest =
        gson.fromJson(testCustomer1Json, Sep12PutCustomerRequest::class.java)
      val senderCustomer = sep12Client.putCustomer(senderCustomerRequest)

      // Create receiver customer
      val receiverCustomerRequest =
        gson.fromJson(testCustomer2Json, Sep12PutCustomerRequest::class.java)
      val receiverCustomer = sep12Client.putCustomer(receiverCustomerRequest)

      val quote = sep38Client.postQuote(asset.sep38, amount, FIAT_USD)

      // POST Sep31 transaction
      val txnRequest = gson.fromJson(postSep31TxnRequest, Sep31PostTransactionRequest::class.java)
      txnRequest.senderId = senderCustomer!!.id
      txnRequest.receiverId = receiverCustomer!!.id
      txnRequest.quoteId = quote.id
      val postTxResponse = sep31Client.postTransaction(txnRequest)

      anchorReferenceServerClient.processSep31Receive(postTxResponse.id)

      // Get transaction status and make sure it is PENDING_SENDER
      val transaction = platformApiClient.getTransaction(postTxResponse.id)
      assertEquals(SepTransactionStatus.PENDING_SENDER, transaction.status)

      val memoType: MemoType =
        when (postTxResponse.stellarMemoType) {
          MemoHelper.memoTypeAsString(org.stellar.sdk.xdr.MemoType.MEMO_ID) -> {
            MemoType.ID
          }
          MemoHelper.memoTypeAsString(org.stellar.sdk.xdr.MemoType.MEMO_HASH) -> {
            MemoType.HASH
          }
          else -> {
            MemoType.TEXT
          }
        }

      // Submit transfer transaction
      val transfer =
        wallet
          .stellar()
          .transaction(keypair)
          .transfer(postTxResponse.stellarAccountId, asset, amount)
          .addMemo(Pair(memoType, postTxResponse.stellarMemo))
          .build()
      transfer.sign(keypair)
      wallet.stellar().submitTransaction(transfer)

      // Wait for the status to change to COMPLETED
      waitStatus(postTxResponse.id, SepTransactionStatus.COMPLETED)

      // Check the events sent to the reference server are recorded correctly
      val actualEvents = waitForBusinessServerEvents(postTxResponse.id, 3)
      assertNotNull(actualEvents)
      actualEvents?.let { assertEquals(3, it.size) }
      val expectedEvents: List<AnchorEvent> =
        gson.fromJson(expectedReceiveEventJson, object : TypeToken<List<AnchorEvent>>() {}.type)
      compareAndAssertEvents(asset, expectedEvents, actualEvents!!)

      // TODO: Investigate why sometimes there are duplicates and different amount of callbacks
      // Check the callbacks sent to the wallet reference server are recorded correctly
      //      val actualCallbacks = waitForWalletServerCallbacks(postTxResponse.id, 3)
      //      actualCallbacks?.let {
      //        assertEquals(3, it.size)
      //        val expectedCallbacks: List<Sep31GetTransactionResponse> =
      //          gson.fromJson(
      //            expectedReceiveCallbacksJson,
      //            object : TypeToken<List<Sep31GetTransactionResponse>>() {}.type
      //          )
      //        compareAndAssertCallbacks(asset, expectedCallbacks, actualCallbacks)
      //      }
    }

  private suspend fun waitForWalletServerCallbacks(
    txnId: String,
    count: Int
  ): List<Sep31GetTransactionResponse>? {
    var retries = 5
    var callbacks: List<Sep31GetTransactionResponse>? = null
    while (retries > 0) {
      callbacks =
        walletServerClient.getCallbackHistory(txnId, Sep31GetTransactionResponse::class.java)
      if (callbacks.size == count) {
        return callbacks
      }
      delay(1.seconds)
      retries--
    }
    return callbacks
  }

  private suspend fun waitForBusinessServerEvents(txnId: String, count: Int): List<AnchorEvent>? {
    var retries = 5
    while (retries > 0) {
      val events = anchorReferenceServerClient.getEvents(txnId)
      if (events.size == count) {
        return events
      }
      delay(1.seconds)
      retries--
    }
    return null
  }

  private suspend fun waitStatus(id: String, expectedStatus: SepTransactionStatus) {
    var status: SepTransactionStatus? = null

    for (i in 0..maxTries) {
      // Get transaction info
      val transaction = platformApiClient.getTransaction(id)

      val current = transaction.status
      info("Expected: $expectedStatus. Current: $current")
      if (status != transaction.status) {
        status = transaction.status
        info("Deposit transaction status changed to $status. Message: ${transaction.message}")
      }

      delay(1.seconds)

      if (transaction.status == expectedStatus) {
        return
      }
    }

    fail("Transaction wasn't $expectedStatus in $maxTries tries, last status: $status")
  }

  fun testAll() {
    info("Running SEP-31 USDC end-to-end tests...")
    `test typical receive end-to-end flow`(USDC, "5")
  }

  companion object {
    private val USDC =
      IssuedAssetId("USDC", "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")
    private const val FIAT_USD = "iso4217:USD"
  }

  private val expectedReceiveEventJson =
    """
 [
   {
     "type": "transaction_created",
     "id": "628ceb6a-5430-4565-a951-ec09fe8d04e4",
     "sep": "31",
     "transaction": {
       "id": "263d8177-f473-4987-9319-4fcd8f8a16b0",
       "sep": "31",
       "kind": "receive",
       "status": "pending_sender",
       "amount_expected": {
         "amount": "5",
         "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
       },
       "amount_in": {
         "amount": "5",
         "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
       },
       "amount_out": {
         "amount": "3.8095",
         "asset": "iso4217:USD"
       },
       "amount_fee": {
         "amount": "1.00",
         "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
       },
       "quote_id": "fb90df1b-34d5-4737-8bee-efd8feeec1dd",
       "started_at": "2023-08-30T15:14:15.047908Z",
       "updated_at": "2023-08-30T15:14:15.051190Z",
       "customers": {
         "sender": {
           "id": "147d1555-bab0-462a-887b-c62edc3f8a65"
         },
         "receiver": {
           "id": "147d1555-bab0-462a-887b-c62edc3f8a65"
         }
       },
       "creator": {
         "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
       }
     }
   },
   {
     "type": "transaction_status_changed",
     "id": "49bde28e-ea68-4227-9e6e-0ed5d8012e56",
     "sep": "31",
     "transaction": {
       "id": "263d8177-f473-4987-9319-4fcd8f8a16b0",
       "sep": "31",
       "kind": "receive",
       "status": "pending_receiver",
       "amount_expected": {
         "amount": "5",
         "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
       },
       "amount_in": {
         "amount": "5.0000000",
         "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
       },
       "amount_out": {
         "amount": "3.8095",
         "asset": "iso4217:USD"
       },
       "amount_fee": {
         "amount": "1.00",
         "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
       },
       "quote_id": "fb90df1b-34d5-4737-8bee-efd8feeec1dd",
       "started_at": "2023-08-30T15:14:15.047908Z",
       "updated_at": "2023-08-30T15:15:11.775070Z",
       "transfer_received_at": "2023-08-30T15:14:21Z",
       "message": "Received an incoming payment",
       "stellar_transactions": [
         {
           "id": "d7a5bfbf09898311d2215156530e267bf2c9aecb8f3273d4de5a2a9cc2c09a6e",
           "memo": "2393666790",
           "memo_type": "id",
           "created_at": "2023-08-30T15:14:21Z",
           "envelope": "AAAAAgAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAGQAABUwAAASvAAAAAEAAAAAAAAAAAAAAABk7119AAAAAgAAAACOrHTmAAAAAQAAAAAAAAABAAAAAEGxI2xgj2aqGDYg25rygTOTzyLGOpz9iKrT/gw0MhwDAAAAAVVTREMAAAAAQj59BfLsr7/sGSshWj8b6WrtuNjnAlSr40E+AgfeVrIAAAAAAvrwgAAAAAAAAAABgRA+VQAAAEBXULBHSyn4FrZCNN5pSccVjjKRAOi+yPuYvrBe9+M3RbNd9VLboMYvqPX6GTNtFo0j9WRPks1MFIE1Kmykh0MB",
           "payments": [
             {
               "id": "5455445984546817",
               "amount": {
                 "amount": "5.0000000",
                 "asset": "USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
               },
               "payment_type": "payment",
               "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
               "destination_account": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6"
             }
           ]
         }
       ],
       "customers": {
         "sender": {
           "id": "147d1555-bab0-462a-887b-c62edc3f8a65"
         },
         "receiver": {
           "id": "147d1555-bab0-462a-887b-c62edc3f8a65"
         }
       },
       "creator": {
         "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
       }
     }
   },
   {
     "type": "transaction_status_changed",
     "id": "3e4e38ec-908a-4def-85e5-11d5b61a866c",
     "sep": "31",
     "transaction": {
       "id": "263d8177-f473-4987-9319-4fcd8f8a16b0",
       "sep": "31",
       "kind": "receive",
       "status": "completed",
       "amount_expected": {
         "amount": "5",
         "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
       },
       "amount_in": {
         "amount": "5.0000000",
         "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
       },
       "amount_out": {
         "amount": "3.8095",
         "asset": "iso4217:USD"
       },
       "amount_fee": {
         "amount": "1.00",
         "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
       },
       "quote_id": "fb90df1b-34d5-4737-8bee-efd8feeec1dd",
       "started_at": "2023-08-30T15:14:15.047908Z",
       "updated_at": "2023-08-30T15:15:17.645248Z",
       "completed_at": "2023-08-30T15:15:17.645244Z",
       "transfer_received_at": "2023-08-30T15:14:21Z",
       "message": "external transfer sent",
       "stellar_transactions": [
         {
           "id": "d7a5bfbf09898311d2215156530e267bf2c9aecb8f3273d4de5a2a9cc2c09a6e",
           "memo": "2393666790",
           "memo_type": "id",
           "created_at": "2023-08-30T15:14:21Z",
           "envelope": "AAAAAgAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAGQAABUwAAASvAAAAAEAAAAAAAAAAAAAAABk7119AAAAAgAAAACOrHTmAAAAAQAAAAAAAAABAAAAAEGxI2xgj2aqGDYg25rygTOTzyLGOpz9iKrT/gw0MhwDAAAAAVVTREMAAAAAQj59BfLsr7/sGSshWj8b6WrtuNjnAlSr40E+AgfeVrIAAAAAAvrwgAAAAAAAAAABgRA+VQAAAEBXULBHSyn4FrZCNN5pSccVjjKRAOi+yPuYvrBe9+M3RbNd9VLboMYvqPX6GTNtFo0j9WRPks1MFIE1Kmykh0MB",
           "payments": [
             {
               "id": "5455445984546817",
               "amount": {
                 "amount": "5.0000000",
                 "asset": "USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
               },
               "payment_type": "payment",
               "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
               "destination_account": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6"
             }
           ]
         }
       ],
       "customers": {
         "sender": {
           "id": "147d1555-bab0-462a-887b-c62edc3f8a65"
         },
         "receiver": {
           "id": "147d1555-bab0-462a-887b-c62edc3f8a65"
         }
       },
       "creator": {
         "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
       }
     }
   }
 ]
  """
      .trimIndent()

  private val postSep31TxnRequest =
    """{
    "amount": "5",
    "asset_code": "USDC",
    "asset_issuer": "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
    "receiver_id": "MOCK_RECEIVER_ID",
    "sender_id": "MOCK_SENDER_ID",
    "fields": {
        "transaction": {
            "receiver_routing_number": "r0123",
            "receiver_account_number": "a0456",
            "type": "SWIFT"
        }
    }
}"""

  private val expectedReceiveCallbacksJson =
    """
[
]
  """
      .trimIndent()
}
