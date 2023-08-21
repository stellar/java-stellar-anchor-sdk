package org.stellar.anchor.platform.test

import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.skyscreamer.jsonassert.JSONAssert
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.platform.CLIENT_WALLET_SECRET
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Log.info
import org.stellar.anchor.util.StringHelper.json
import org.stellar.reference.client.AnchorReferenceServerClient
import org.stellar.walletsdk.ApplicationConfiguration
import org.stellar.walletsdk.StellarConfiguration
import org.stellar.walletsdk.Wallet
import org.stellar.walletsdk.anchor.DepositTransaction
import org.stellar.walletsdk.anchor.TransactionStatus
import org.stellar.walletsdk.anchor.TransactionStatus.*
import org.stellar.walletsdk.anchor.WithdrawalTransaction
import org.stellar.walletsdk.asset.IssuedAssetId
import org.stellar.walletsdk.asset.StellarAssetId
import org.stellar.walletsdk.asset.XLM
import org.stellar.walletsdk.auth.AuthToken
import org.stellar.walletsdk.horizon.SigningKeyPair
import org.stellar.walletsdk.horizon.sign
import org.stellar.walletsdk.horizon.transaction.transferWithdrawalTransaction

class Sep24CustodyRpcEnd2EndTests(config: TestConfig, val jwt: String) {
  private val gson = GsonUtils.getInstance()
  private val walletSecretKey = System.getenv("WALLET_SECRET_KEY") ?: CLIENT_WALLET_SECRET
  private val keypair = SigningKeyPair.fromSecret(walletSecretKey)
  private val wallet =
    Wallet(
      StellarConfiguration.Testnet,
      ApplicationConfiguration { defaultRequest { url { protocol = URLProtocol.HTTP } } }
    )
  private val client = HttpClient {
    install(HttpTimeout) {
      requestTimeoutMillis = 300000
      connectTimeoutMillis = 300000
      socketTimeoutMillis = 300000
    }
  }
  private val anchor =
    wallet.anchor(config.env["anchor.domain"]!!) {
      install(HttpTimeout) {
        requestTimeoutMillis = 300000
        connectTimeoutMillis = 300000
        socketTimeoutMillis = 300000
      }
    }
  private val maxTries = 60
  private val anchorReferenceServerClient =
    AnchorReferenceServerClient(Url(config.env["reference.server.url"]!!))

  private fun `test typical deposit end-to-end flow`(asset: StellarAssetId, amount: String) =
    runBlocking {
      val token = anchor.auth().authenticate(keypair)
      val txnId = makeDeposit(asset, amount, token)
      // Wait for the status to change to COMPLETED
      waitStatus(txnId, COMPLETED, token)

      // Check if the transaction can be listed by stellar transaction id
      val fetchedTxn = anchor.getTransaction(txnId, token) as DepositTransaction
      val transactionByStellarId =
        anchor.getTransactionBy(token, stellarTransactionId = fetchedTxn.stellarTransactionId)
      assertEquals(fetchedTxn.id, transactionByStellarId.id)

      // Check the events sent to the reference server are recorded correctly
      val actualEvents = waitForEvents(txnId, 5)
      assertNotNull(actualEvents)
      actualEvents?.let { assertEquals(5, it.size) }
      val expectedEvents: List<AnchorEvent> =
        gson.fromJson(expectedDepositEventsJson, object : TypeToken<List<AnchorEvent>>() {}.type)
      compareAndAssertEvents(asset, expectedEvents, actualEvents!!)
    }

  private suspend fun makeDeposit(asset: StellarAssetId, amount: String, token: AuthToken): String {
    // Start interactive deposit
    val deposit = anchor.interactive().deposit(asset, token, mapOf("amount" to amount))
    // Get transaction status and make sure it is INCOMPLETE
    val transaction = anchor.getTransaction(deposit.id, token)
    assertEquals(INCOMPLETE, transaction.status)
    // Make sure the interactive url is valid. This will also start the reference server's
    // withdrawal process.
    val resp = client.get(deposit.url)
    info("accessing ${deposit.url}...")
    assertEquals(200, resp.status.value)

    return transaction.id
  }

  // TODO: Validate assets of expected and actual events
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
        expectedEvent.transaction.stellarTransactions = actualEvent.transaction.stellarTransactions
        expectedEvent.transaction.memo = actualEvent.transaction.memo
        expectedEvent.transaction.amountExpected.asset = asset.id
        expectedEvent.transaction.completedAt = actualEvent.transaction.completedAt
        actualEvent.transaction.amountIn?.let {
          expectedEvent.transaction.amountIn.amount = actualEvent.transaction.amountIn.amount
          //          expectedEvent.transaction.amountIn.asset = asset.sep38
          expectedEvent.transaction.amountIn.asset = actualEvent.transaction.amountIn.asset
        }
        actualEvent.transaction.amountOut?.let {
          expectedEvent.transaction.amountOut.amount = actualEvent.transaction.amountOut.amount
          //          expectedEvent.transaction.amountOut.asset = asset.sep38
          expectedEvent.transaction.amountOut.asset = actualEvent.transaction.amountOut.asset
        }
        actualEvent.transaction.amountFee?.let {
          expectedEvent.transaction.amountFee.amount = actualEvent.transaction.amountFee.amount
          //          expectedEvent.transaction.amountFee.asset = asset.sep38
          expectedEvent.transaction.amountFee.asset = actualEvent.transaction.amountFee.asset
        }
        actualEvent.transaction.amountExpected?.let {
          expectedEvent.transaction.amountExpected.amount =
            actualEvent.transaction.amountExpected.amount
          //          expectedEvent.transaction.amountExpected.asset = asset.sep38
          expectedEvent.transaction.amountExpected.asset =
            actualEvent.transaction.amountExpected.asset
        }

        expectedEvent.transaction.destinationAccount = actualEvent.transaction.destinationAccount
      }
    }
    JSONAssert.assertEquals(json(expectedEvents), gson.toJson(actualEvents), true)
  }

  private fun `test typical withdraw end-to-end flow`(asset: StellarAssetId, amount: String) {
    `test typical withdraw end-to-end flow`(asset, mapOf("amount" to amount))
  }

  private fun `test typical withdraw end-to-end flow`(
    asset: StellarAssetId,
    extraFields: Map<String, String>
  ) = runBlocking {
    val token = anchor.auth().authenticate(keypair)
    // TODO: Add the test where the amount is not specified
    //    val withdrawal = anchor.interactive().withdraw(keypair.address, asset, token)
    // Start interactive withdrawal
    val withdrawTxn = anchor.interactive().withdraw(asset, token, extraFields)

    // Get transaction status and make sure it is INCOMPLETE
    val transaction = anchor.getTransaction(withdrawTxn.id, token)
    assertEquals(INCOMPLETE, transaction.status)
    // Make sure the interactive url is valid. This will also start the reference server's
    // withdrawal process.
    val resp = client.get(withdrawTxn.url)
    info("accessing ${withdrawTxn.url}...")
    assertEquals(200, resp.status.value)
    // Wait for the status to change to PENDING_USER_TRANSFER_START
    waitStatus(withdrawTxn.id, PENDING_USER_TRANSFER_START, token)
    // Submit transfer transaction
    val walletTxn = (anchor.getTransaction(withdrawTxn.id, token) as WithdrawalTransaction)
    val transfer =
      wallet
        .stellar()
        .transaction(walletTxn.from)
        .transferWithdrawalTransaction(walletTxn, asset)
        .build()
    transfer.sign(keypair)
    wallet.stellar().submitTransaction(transfer)
    // Wait for the status to change to PENDING_USER_TRANSFER_END
    waitStatus(withdrawTxn.id, COMPLETED, token)

    // Check if the transaction can be listed by stellar transaction id
    val fetchTxn = anchor.getTransaction(withdrawTxn.id, token) as WithdrawalTransaction
    val transactionByStellarId =
      anchor.getTransactionBy(token, stellarTransactionId = fetchTxn.stellarTransactionId)
    assertEquals(fetchTxn.id, transactionByStellarId.id)

    // Check the events sent to the reference server are recorded correctly
    val actualEvents = waitForEvents(withdrawTxn.id, 4)
    assertNotNull(actualEvents)
    actualEvents?.let { assertEquals(4, it.size) }
    val expectedEvents: List<AnchorEvent> =
      gson.fromJson(expectedWithdrawEventJson, object : TypeToken<List<AnchorEvent>>() {}.type)
    compareAndAssertEvents(asset, expectedEvents, actualEvents!!)
  }

  private suspend fun waitForEvents(txnId: String, count: Int): List<AnchorEvent>? {
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

  private suspend fun waitStatus(id: String, expectedStatus: TransactionStatus, token: AuthToken) {
    var status: TransactionStatus? = null

    for (i in 0..maxTries) {
      // Get transaction info
      val transaction = anchor.getTransaction(id, token)

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

  private fun `test created transactions show up in the get history call`(
    asset: StellarAssetId,
    amount: String
  ) = runBlocking {
    val newAcc = wallet.stellar().account().createKeyPair()

    val tx =
      wallet
        .stellar()
        .transaction(keypair)
        .sponsoring(keypair, newAcc) {
          createAccount(newAcc)
          addAssetSupport(USDC)
        }
        .build()
        .sign(keypair)
        .sign(newAcc)

    wallet.stellar().submitTransaction(tx)

    val token = anchor.auth().authenticate(newAcc)
    val deposits =
      (0..1).map {
        val txnId = makeDeposit(asset, amount, token)
        waitStatus(txnId, COMPLETED, token)
        txnId
      }
    val history = anchor.getHistory(asset, token)

    Assertions.assertThat(history).allMatch { deposits.contains(it.id) }
  }

  fun testAll() {
    info("Running SEP-24 USDC end-to-end tests...")
    `test typical deposit end-to-end flow`(USDC, "5")
    `test typical withdraw end-to-end flow`(USDC, "5")
    `test created transactions show up in the get history call`(USDC, "5")
    info("Running SEP-24 XLM end-to-end tests...")
    `test typical deposit end-to-end flow`(XLM, "0.00001")
    `test typical withdraw end-to-end flow`(XLM, "0.00001")
  }

  companion object {
    private val USDC =
      IssuedAssetId("USDC", "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")

    val expectedDepositEventsJson =
      """
[
  {
    "type": "TRANSACTION_CREATED",
    "id": "b4dc4fce-9489-4bfe-ab76-86ae91aeca18",
    "sep": "24",
    "transaction": {
      "id": "80d58cc0-aa9b-4b97-89f7-64ad16b8620a",
      "sep": "24",
      "kind": "deposit",
      "status": "incomplete",
      "amount_expected": {
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-01T13:31:06.286427Z",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "type": "TRANSACTION_STATUS_CHANGED",
    "id": "eb812d80-2e83-4345-913d-6b0405db6515",
    "sep": "24",
    "transaction": {
      "id": "80d58cc0-aa9b-4b97-89f7-64ad16b8620a",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_user_transfer_start",
      "amount_expected": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "4.5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "0.5",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-01T13:31:06.286427Z",
      "updated_at": "2023-08-01T13:31:08.559929Z",
      "message": "waiting on the user to transfer funds",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "type": "TRANSACTION_STATUS_CHANGED",
    "id": "61bfadd3-9afc-4dd3-bed9-1ae03315340d",
    "sep": "24",
    "transaction": {
      "id": "80d58cc0-aa9b-4b97-89f7-64ad16b8620a",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_anchor",
      "amount_expected": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "4.5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "0.5",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-01T13:31:06.286427Z",
      "updated_at": "2023-08-01T13:31:09.720584Z",
      "message": "funds received, transaction is being processed",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "type": "TRANSACTION_STATUS_CHANGED",
    "id": "8833556b-997c-4198-8043-f2885628cba7",
    "sep": "24",
    "transaction": {
      "id": "80d58cc0-aa9b-4b97-89f7-64ad16b8620a",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_stellar",
      "amount_expected": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "4.5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "0.5",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-01T13:31:06.286427Z",
      "updated_at": "2023-08-01T13:31:13.176820Z",
      "message": "funds received, transaction is being processed",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "type": "TRANSACTION_STATUS_CHANGED",
    "id": "e3ede86c-265a-455e-a432-c0e19d123559",
    "sep": "24",
    "transaction": {
      "id": "80d58cc0-aa9b-4b97-89f7-64ad16b8620a",
      "sep": "24",
      "kind": "deposit",
      "status": "completed",
      "amount_expected": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "5",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "4.5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "0.5",
        "asset": "iso4217:USD"
      },
      "started_at": "2023-08-01T13:31:06.286427Z",
      "updated_at": "2023-08-01T13:31:31.065213Z",
      "completed_at": "2023-08-01T13:31:31.065218Z",
      "message": "Outgoing payment sent",
      "stellar_transactions": [
        {
          "id": "090d730200c58e85bbc99c2a916d268fc7a4c43ed9614fb988b45b4182f594ba",
          "created_at": "2023-08-01T13:31:24Z",
          "envelope": "AAAAAgAAAABBsSNsYI9mqhg2INua8oEzk88ixjqc/Yiq0/4MNDIcAwAPQkAAAcGcAAABuwAAAAEAAAAAFL/7JgAAAABkyTNQAAAAAAAAAAEAAAAAAAAAAQAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAAFVU0RDAAAAAEI+fQXy7K+/7BkrIVo/G+lq7bjY5wJUq+NBPgIH3layAAAAAAKupUAAAAAAAAAAATQyHAMAAABARPxvOFTgreBPEdvlz+rrtWFu94ZxNUy/5vC78DOrfUOw6UebWM8Dxnhhas9081zR27g3TboewX+TJXqA+BhnBg==",
          "payments": [
            {
              "id": "3402902588628993",
              "amount": {
                "amount": "4.5000000",
                "asset": "USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
              },
              "payment_type": "payment",
              "source_account": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6",
              "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
            }
          ]
        }
      ],
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  }
]
  """
        .trimIndent()
  }

  private val expectedWithdrawEventJson =
    """
    [
      {
        "type": "TRANSACTION_CREATED",
        "id": "c9f4c97c-2114-42c5-b8b1-0590492b0cd2",
        "sep": "24",
        "transaction": {
          "id": "7483d380-d6ff-40a5-b681-1738ba87dc6a",
          "sep": "24",
          "kind": "withdrawal",
          "status": "incomplete",
          "amount_expected": {
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "started_at": "2023-08-01T13:34:14.635709Z",
          "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
          "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF"
        }
      },
      {
        "type": "TRANSACTION_STATUS_CHANGED",
        "id": "efe63d6b-fa2f-467c-8175-8edfb01545ad",
        "sep": "24",
        "transaction": {
          "id": "7483d380-d6ff-40a5-b681-1738ba87dc6a",
          "sep": "24",
          "kind": "withdrawal",
          "status": "pending_user_transfer_start",
          "amount_expected": {
            "amount": "5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_in": {
            "amount": "5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_out": {
            "amount": "4.5",
            "asset": "iso4217:USD"
          },
          "amount_fee": {
            "amount": "0.5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "started_at": "2023-08-01T13:34:14.635709Z",
          "updated_at": "2023-08-01T13:34:16.132707Z",
          "message": "waiting on the user to transfer funds",
          "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
          "destination_account": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6",
          "memo": "1609894126",
          "memo_type": "id"
        }
      },
      {
        "type": "TRANSACTION_STATUS_CHANGED",
        "id": "3b0326c5-08d7-4dba-96d0-d46f2931e91a",
        "sep": "24",
        "transaction": {
          "id": "7483d380-d6ff-40a5-b681-1738ba87dc6a",
          "sep": "24",
          "kind": "withdrawal",
          "status": "pending_anchor",
          "amount_expected": {
            "amount": "5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_in": {
            "amount": "5.0000000",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_out": {
            "amount": "4.5",
            "asset": "iso4217:USD"
          },
          "amount_fee": {
            "amount": "0.5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "started_at": "2023-08-01T13:34:14.635709Z",
          "updated_at": "2023-08-01T13:34:41.157032Z",
          "message": "Received an incoming payment",
          "stellar_transactions": [
            {
              "id": "682432006a60477b6e177dd45473fc1c94529b6a3db71bb3d5be5f32592f81fd",
              "memo": "1609894126",
              "memo_type": "id",
              "created_at": "2023-08-01T13:34:23Z",
              "envelope": "AAAAAgAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAGQAABUwAAAKZgAAAAEAAAAAAAAAAAAAAABkyQqPAAAAAgAAAABf9QjuAAAAAQAAAAAAAAABAAAAAEGxI2xgj2aqGDYg25rygTOTzyLGOpz9iKrT/gw0MhwDAAAAAVVTREMAAAAAQj59BfLsr7/sGSshWj8b6WrtuNjnAlSr40E+AgfeVrIAAAAAAvrwgAAAAAAAAAABgRA+VQAAAEAupvXbutbIyJAVEgEPi5CTNL1LUJiNkqjIGEQ8tDiJ3aZFTahJWeck6gFQrmrfNorHpwNGBIfvCvhTRw26L8sE",
              "payments": [
                {
                  "id": "3403048617512961",
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
          "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
          "destination_account": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6",
          "memo": "1609894126",
          "memo_type": "id"
        }
      },
      {
        "type": "TRANSACTION_STATUS_CHANGED",
        "id": "60df3832-4f86-4fd3-8df3-0253a4c81f82",
        "sep": "24",
        "transaction": {
          "id": "7483d380-d6ff-40a5-b681-1738ba87dc6a",
          "sep": "24",
          "kind": "withdrawal",
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
            "amount": "4.5",
            "asset": "iso4217:USD"
          },
          "amount_fee": {
            "amount": "0.5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "started_at": "2023-08-01T13:34:14.635709Z",
          "updated_at": "2023-08-01T13:34:42.316757Z",
          "completed_at": "2023-08-01T13:34:42.316760Z",
          "message": "pending external transfer",
          "stellar_transactions": [
            {
              "id": "682432006a60477b6e177dd45473fc1c94529b6a3db71bb3d5be5f32592f81fd",
              "memo": "1609894126",
              "memo_type": "id",
              "created_at": "2023-08-01T13:34:23Z",
              "envelope": "AAAAAgAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAGQAABUwAAAKZgAAAAEAAAAAAAAAAAAAAABkyQqPAAAAAgAAAABf9QjuAAAAAQAAAAAAAAABAAAAAEGxI2xgj2aqGDYg25rygTOTzyLGOpz9iKrT/gw0MhwDAAAAAVVTREMAAAAAQj59BfLsr7/sGSshWj8b6WrtuNjnAlSr40E+AgfeVrIAAAAAAvrwgAAAAAAAAAABgRA+VQAAAEAupvXbutbIyJAVEgEPi5CTNL1LUJiNkqjIGEQ8tDiJ3aZFTahJWeck6gFQrmrfNorHpwNGBIfvCvhTRw26L8sE",
              "payments": [
                {
                  "id": "3403048617512961",
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
          "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
          "destination_account": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6",
          "memo": "1609894126",
          "memo_type": "id"
        }
      }
    ]
  """
      .trimIndent()
}
