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

class Sep24End2EndTest(config: TestConfig, val jwt: String) {
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
  private val maxTries = 30
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
      val actualEvents = waitForEvents(txnId, 4)
      assertNotNull(actualEvents)
      actualEvents?.let { assertEquals(4, it.size) }
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
        actualEvent.transaction.amountIn?.let {
          expectedEvent.transaction.amountIn.amount = actualEvent.transaction.amountIn.amount
          expectedEvent.transaction.amountIn.asset = asset.sep38
        }
        actualEvent.transaction.amountOut?.let {
          expectedEvent.transaction.amountOut.amount = actualEvent.transaction.amountOut.amount
          expectedEvent.transaction.amountOut.asset = asset.sep38
        }
        actualEvent.transaction.amountFee?.let {
          expectedEvent.transaction.amountFee.amount = actualEvent.transaction.amountFee.amount
          expectedEvent.transaction.amountFee.asset = asset.sep38
        }
        actualEvent.transaction.amountExpected?.let {
          expectedEvent.transaction.amountExpected.amount =
            actualEvent.transaction.amountExpected.amount
          expectedEvent.transaction.amountExpected.asset = asset.sep38
        }
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
    val actualEvents = waitForEvents(withdrawTxn.id, 5)
    assertNotNull(actualEvents)
    actualEvents?.let { assertEquals(5, it.size) }
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
      IssuedAssetId("USDC", "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP")

    val expectedDepositEventsJson =
      """
[
  {
    "type": "TRANSACTION_CREATED",
    "sep": "24",
    "transaction": {
      "sep": "24",
      "kind": "deposit",
      "status": "incomplete",
      "amount_expected": {
      },
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "type": "TRANSACTION_STATUS_CHANGED",
    "sep": "24",
    "transaction": {
      "sep": "24",
      "kind": "deposit",
      "status": "pending_user_transfer_start",
      "amount_expected": {
      },
      "amount_in": {
      },
      "amount_out": {
      },
      "amount_fee": {
      },
      "message": "waiting on the user to transfer funds",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "memo_type": "hash"
    }
  },
  {
    "type": "TRANSACTION_STATUS_CHANGED",
    "sep": "24",
    "transaction": {
      "sep": "24",
      "kind": "deposit",
      "status": "pending_anchor",
      "amount_expected": {
      },
      "amount_in": {
      },
      "amount_out": {
      },
      "amount_fee": {
      },
      "message": "funds received, transaction is being processed",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "memo_type": "hash"
    }
  },
  {
    "type": "TRANSACTION_STATUS_CHANGED",
    "sep": "24",
    "transaction": {
      "sep": "24",
      "kind": "deposit",
      "status": "completed",
      "amount_expected": {
      },
      "amount_in": {
      },
      "amount_out": {
      },
      "amount_fee": {
      },
      "message": "completed",
      "stellar_transactions": [
        {
          "id": "111129e48806cdc4873c98e769c8c736a0d157d4e6a24c5ecb4c64b3b0e4a890",
          "payments": [
            {
              "id": "2499297304129537",
              "amount": {
              }
            }
          ]
        }
      ],
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "memo_type": "hash"
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
        "sep": "24",
        "transaction": {
          "sep": "24",
          "kind": "withdrawal",
          "status": "incomplete",
          "amount_expected": {
          },
          "started_at": "2023-07-19T20:20:51.792908200Z",
          "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
          "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF"
        }
      },
      {
        "type": "TRANSACTION_STATUS_CHANGED",
        "sep": "24",
        "transaction": {
          "sep": "24",
          "kind": "withdrawal",
          "status": "pending_user_transfer_start",
          "amount_expected": {
          },
          "amount_in": {
          },
          "amount_out": {
          },
          "amount_fee": {
          },
          "message": "waiting on the user to transfer funds",
          "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
          "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
          "memo_type": "hash"
        }
      },
      {
        "type": "TRANSACTION_STATUS_CHANGED",
        "id": "f0b75f9e-1b53-442a-91dd-d6c002a51bfc",
        "sep": "24",
        "transaction": {
          "id": "14882409-757c-4c66-9da1-3dddef11953a",
          "sep": "24",
          "kind": "withdrawal",
          "status": "pending_anchor",
          "amount_expected": {
          },
          "amount_in": {
          },
          "amount_out": {
          },
          "amount_fee": {
          },
          "message": "waiting on the user to transfer funds",
          "stellar_transactions": [
            {
              "id": "9234bd186612f4d48cafed4c702509f680a581c3e02945f0206b4c8ac627b83a",
              "memo": "MTQ4ODI0MDktNzU3Yy00YzY2LTlkYTEtM2RkZGVmMTE\u003d",
              "memo_type": "hash",
              "created_at": "2023-07-19T20:21:01Z",
              "envelope": "AAAAAgAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAGQAABUwAAAGzgAAAAEAAAAAAAAAAAAAAABkuEZbAAAAAzE0ODgyNDA5LTc1N2MtNGM2Ni05ZGExLTNkZGRlZjExAAAAAQAAAAAAAAABAAAAAFvGtEMyXcvbioU2IKCSomxahpl7lUyef7ftEPxWcD4bAAAAAVVTREMAAAAA4OJrYiyoyVYK5jqTvfhX91wJp8nB8jVCrv7/SoR3rwAAAAAABfXhAAAAAAAAAAABgRA+VQAAAEDlmaoq46tJ7Lja9SP4BAuTl1GOrPuf7HAsK4JyNdhxkwz2p5U181Eq394rjIn/fr43lkgarA9m05Q04t4gHqkH",
              "payments": [
                {
                  "id": "2504876466638849",
                  "amount": {
                  },
                  "payment_type": "payment",
                  "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
                  "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF"
                }
              ]
            }
          ],
          "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
          "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
          "memo_type": "hash"
        }
      },
      {
        "type": "TRANSACTION_STATUS_CHANGED",
        "id": "b44d90ec-2d9a-4768-a952-085026f5b3da",
        "sep": "24",
        "transaction": {
          "id": "14882409-757c-4c66-9da1-3dddef11953a",
          "sep": "24",
          "kind": "withdrawal",
          "status": "pending_external",
          "amount_expected": {
          },
          "amount_in": {
          },
          "amount_out": {
          },
          "amount_fee": {
          },
          "message": "pending external transfer",
          "stellar_transactions": [
            {
              "id": "9234bd186612f4d48cafed4c702509f680a581c3e02945f0206b4c8ac627b83a",
              "memo": "MTQ4ODI0MDktNzU3Yy00YzY2LTlkYTEtM2RkZGVmMTE\u003d",
              "memo_type": "hash",
              "created_at": "2023-07-19T20:21:01Z",
              "envelope": "AAAAAgAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAGQAABUwAAAGzgAAAAEAAAAAAAAAAAAAAABkuEZbAAAAAzE0ODgyNDA5LTc1N2MtNGM2Ni05ZGExLTNkZGRlZjExAAAAAQAAAAAAAAABAAAAAFvGtEMyXcvbioU2IKCSomxahpl7lUyef7ftEPxWcD4bAAAAAVVTREMAAAAA4OJrYiyoyVYK5jqTvfhX91wJp8nB8jVCrv7/SoR3rwAAAAAABfXhAAAAAAAAAAABgRA+VQAAAEDlmaoq46tJ7Lja9SP4BAuTl1GOrPuf7HAsK4JyNdhxkwz2p5U181Eq394rjIn/fr43lkgarA9m05Q04t4gHqkH",
              "payments": [
                {
                  "id": "2504876466638849",
                  "amount": {
                  },
                  "payment_type": "payment",
                  "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
                  "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF"
                }
              ]
            }
          ],
          "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
          "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
          "memo_type": "hash"
        }
      },
      {
        "type": "TRANSACTION_STATUS_CHANGED",
        "id": "0556b75c-b054-49a0-b778-654050a6cba4",
        "sep": "24",
        "transaction": {
          "id": "14882409-757c-4c66-9da1-3dddef11953a",
          "sep": "24",
          "kind": "withdrawal",
          "status": "completed",
          "amount_expected": {
          },
          "amount_in": {
          },
          "amount_out": {
          },
          "amount_fee": {
          },
          "message": "completed",
          "stellar_transactions": [
            {
              "id": "9234bd186612f4d48cafed4c702509f680a581c3e02945f0206b4c8ac627b83a",
              "memo": "MTQ4ODI0MDktNzU3Yy00YzY2LTlkYTEtM2RkZGVmMTE\u003d",
              "memo_type": "hash",
              "created_at": "2023-07-19T20:21:01Z",
              "envelope": "AAAAAgAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAGQAABUwAAAGzgAAAAEAAAAAAAAAAAAAAABkuEZbAAAAAzE0ODgyNDA5LTc1N2MtNGM2Ni05ZGExLTNkZGRlZjExAAAAAQAAAAAAAAABAAAAAFvGtEMyXcvbioU2IKCSomxahpl7lUyef7ftEPxWcD4bAAAAAVVTREMAAAAA4OJrYiyoyVYK5jqTvfhX91wJp8nB8jVCrv7/SoR3rwAAAAAABfXhAAAAAAAAAAABgRA+VQAAAEDlmaoq46tJ7Lja9SP4BAuTl1GOrPuf7HAsK4JyNdhxkwz2p5U181Eq394rjIn/fr43lkgarA9m05Q04t4gHqkH",
              "payments": [
                {
                  "id": "2504876466638849",
                  "amount": {
                  },
                  "payment_type": "payment",
                  "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
                  "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF"
                }
              ]
            }
          ],
          "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
          "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
          "memo_type": "hash"
        }
      }
    ]
  """
      .trimIndent()
}
