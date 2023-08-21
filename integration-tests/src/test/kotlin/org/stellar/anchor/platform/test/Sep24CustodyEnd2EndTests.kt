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

class Sep24CustodyEnd2EndTests(config: TestConfig, val jwt: String) {
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
      val actualEvents = waitForEvents(txnId, 6)
      assertNotNull(actualEvents)
      actualEvents?.let { assertEquals(6, it.size) }
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
        expectedEvent.transaction.completedAt = actualEvent.transaction.completedAt
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
      IssuedAssetId("USDC", "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")

    val expectedDepositEventsJson =
      """
[
  {
    "type": "TRANSACTION_CREATED",
    "id": "c5304280-22d8-4ed0-9c90-15e83d64f276",
    "sep": "24",
    "transaction": {
      "id": "a26566ce-f13b-4130-ac16-784787fc1cc3",
      "sep": "24",
      "kind": "deposit",
      "status": "incomplete",
      "amount_expected": {
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-01T12:41:59.244202Z",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "type": "TRANSACTION_STATUS_CHANGED",
    "id": "c1cf2492-bd1e-45b4-834a-0bf753e06705",
    "sep": "24",
    "transaction": {
      "id": "a26566ce-f13b-4130-ac16-784787fc1cc3",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_user_transfer_start",
      "amount_expected": {
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "4.5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "0.5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-01T12:41:59.244202Z",
      "updated_at": "2023-08-01T12:42:00.538476Z",
      "message": "waiting on the user to transfer funds",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "type": "TRANSACTION_STATUS_CHANGED",
    "id": "5780c057-6744-4c63-bac4-537c095462d1",
    "sep": "24",
    "transaction": {
      "id": "a26566ce-f13b-4130-ac16-784787fc1cc3",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_anchor",
      "amount_expected": {
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "4.5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "0.5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-01T12:41:59.244202Z",
      "updated_at": "2023-08-01T12:42:01.635123Z",
      "message": "funds received, transaction is being processed",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "type": "TRANSACTION_STATUS_CHANGED",
    "id": "3ea14465-ec81-4403-8a38-a25494574603",
    "sep": "24",
    "transaction": {
      "id": "a26566ce-f13b-4130-ac16-784787fc1cc3",
      "sep": "24",
      "kind": "deposit",
      "status": "pending_stellar",
      "amount_expected": {
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "4.5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "0.5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-01T12:41:59.244202Z",
      "updated_at": "2023-08-01T12:42:02.698120Z",
      "message": "funds received, transaction is being processed",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "type": "TRANSACTION_STATUS_CHANGED",
    "id": "0908fca8-dbf1-475e-ab2b-d015deab08f0",
    "sep": "24",
    "transaction": {
      "id": "a26566ce-f13b-4130-ac16-784787fc1cc3",
      "sep": "24",
      "kind": "deposit",
      "status": "completed",
      "amount_expected": {
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "4.5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "0.5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-01T12:41:59.244202Z",
      "updated_at": "2023-08-01T12:42:21.342756Z",
      "completed_at": "2023-08-01T12:42:21.342759Z",
      "message": "Outgoing payment sent",
      "stellar_transactions": [
        {
          "id": "5029e54c39b02896fe593c31ef2f5de20cba19bf725a719c104aee603c15fa92",
          "created_at": "2023-08-01T12:42:12Z",
          "envelope": "AAAAAgAAAABBsSNsYI9mqhg2INua8oEzk88ixjqc/Yiq0/4MNDIcAwAAOpgAAcGcAAABqQAAAAEAAAAAGw2kqAAAAABkySfMAAAAAAAAAAEAAAAAAAAAAQAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAAFVU0RDAAAAAEI+fQXy7K+/7BkrIVo/G+lq7bjY5wJUq+NBPgIH3layAAAAAAKupUAAAAAAAAAAATQyHAMAAABAJd2DOcpj8T29YdQ1K4yTdej54qzG7LCcOSHsnS1fPdXJd5j84bdiIVvXKs9F5K3fcM3D44Z3q+ixUw3TGHHNAw==",
          "payments": [
            {
              "id": "3400484522037249",
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
  },
  {
    "type": "TRANSACTION_STATUS_CHANGED",
    "id": "8067eb53-2d1c-4ff3-92d9-929a77bba8d9",
    "sep": "24",
    "transaction": {
      "id": "a26566ce-f13b-4130-ac16-784787fc1cc3",
      "sep": "24",
      "kind": "deposit",
      "status": "completed",
      "amount_expected": {
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "4.5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "0.5",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-01T12:41:59.244202Z",
      "updated_at": "2023-08-01T12:42:24.985080Z",
      "completed_at": "2023-08-01T12:42:21.342759Z",
      "message": "completed",
      "stellar_transactions": [
        {
          "id": "5029e54c39b02896fe593c31ef2f5de20cba19bf725a719c104aee603c15fa92",
          "created_at": "2023-08-01T12:42:12Z",
          "envelope": "AAAAAgAAAABBsSNsYI9mqhg2INua8oEzk88ixjqc/Yiq0/4MNDIcAwAAOpgAAcGcAAABqQAAAAEAAAAAGw2kqAAAAABkySfMAAAAAAAAAAEAAAAAAAAAAQAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAAFVU0RDAAAAAEI+fQXy7K+/7BkrIVo/G+lq7bjY5wJUq+NBPgIH3layAAAAAAKupUAAAAAAAAAAATQyHAMAAABAJd2DOcpj8T29YdQ1K4yTdej54qzG7LCcOSHsnS1fPdXJd5j84bdiIVvXKs9F5K3fcM3D44Z3q+ixUw3TGHHNAw==",
          "payments": [
            {
              "id": "3400484522037249",
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
        "id": "8b71f130-3d8f-4a39-b9b4-5f7c5c8664e8",
        "sep": "24",
        "transaction": {
          "id": "24dcb15c-40a8-4636-ae5a-6fb76b80c1c1",
          "sep": "24",
          "kind": "withdrawal",
          "status": "incomplete",
          "amount_expected": {
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "started_at": "2023-08-01T13:14:46.687761Z",
          "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
          "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF"
        }
      },
      {
        "type": "TRANSACTION_STATUS_CHANGED",
        "id": "32211789-2025-4660-9718-4dc2b97b5145",
        "sep": "24",
        "transaction": {
          "id": "24dcb15c-40a8-4636-ae5a-6fb76b80c1c1",
          "sep": "24",
          "kind": "withdrawal",
          "status": "pending_user_transfer_start",
          "amount_expected": {
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_in": {
            "amount": "5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_out": {
            "amount": "4.5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_fee": {
            "amount": "0.5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "started_at": "2023-08-01T13:14:46.687761Z",
          "updated_at": "2023-08-01T13:14:47.766747Z",
          "message": "waiting on the user to transfer funds",
          "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
          "destination_account": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6",
          "memo": "3802820112",
          "memo_type": "id"
        }
      },
      {
        "type": "TRANSACTION_STATUS_CHANGED",
        "id": "394e762f-24b2-4b49-8027-0f2744a5e6e8",
        "sep": "24",
        "transaction": {
          "id": "24dcb15c-40a8-4636-ae5a-6fb76b80c1c1",
          "sep": "24",
          "kind": "withdrawal",
          "status": "pending_anchor",
          "amount_expected": {
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_in": {
            "amount": "5.0000000",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_out": {
            "amount": "4.5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_fee": {
            "amount": "0.5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "started_at": "2023-08-01T13:14:46.687761Z",
          "updated_at": "2023-08-01T13:15:11.186631Z",
          "message": "Received an incoming payment",
          "stellar_transactions": [
            {
              "id": "32969862db2b562832d331cd418b3ad96d82bd7f8dec8d7e684e86db489b6892",
              "memo": "3802820112",
              "memo_type": "id",
              "created_at": "2023-08-01T13:14:54Z",
              "envelope": "AAAAAgAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAGQAABUwAAAKYQAAAAEAAAAAAAAAAAAAAABkyQX9AAAAAgAAAADiqm4QAAAAAQAAAAAAAAABAAAAAEGxI2xgj2aqGDYg25rygTOTzyLGOpz9iKrT/gw0MhwDAAAAAVVTREMAAAAAQj59BfLsr7/sGSshWj8b6WrtuNjnAlSr40E+AgfeVrIAAAAAAvrwgAAAAAAAAAABgRA+VQAAAECboQvzfHhL04WnLPMfhCJoeT8ozvEnhwuYbm8oQZP8CwLbKBvN+d+sm85TB/Eb8iYj5SoB4SKo6Gww44aLot0G",
              "payments": [
                {
                  "id": "3402095134773249",
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
          "memo": "3802820112",
          "memo_type": "id"
        }
      },
      {
        "type": "TRANSACTION_STATUS_CHANGED",
        "id": "e50c8773-045d-4a70-8551-711ff1428ca7",
        "sep": "24",
        "transaction": {
          "id": "24dcb15c-40a8-4636-ae5a-6fb76b80c1c1",
          "sep": "24",
          "kind": "withdrawal",
          "status": "pending_external",
          "amount_expected": {
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_in": {
            "amount": "5.0000000",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_out": {
            "amount": "4.5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_fee": {
            "amount": "0.5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "started_at": "2023-08-01T13:14:46.687761Z",
          "updated_at": "2023-08-01T13:15:14.275683Z",
          "message": "pending external transfer",
          "stellar_transactions": [
            {
              "id": "32969862db2b562832d331cd418b3ad96d82bd7f8dec8d7e684e86db489b6892",
              "memo": "3802820112",
              "memo_type": "id",
              "created_at": "2023-08-01T13:14:54Z",
              "envelope": "AAAAAgAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAGQAABUwAAAKYQAAAAEAAAAAAAAAAAAAAABkyQX9AAAAAgAAAADiqm4QAAAAAQAAAAAAAAABAAAAAEGxI2xgj2aqGDYg25rygTOTzyLGOpz9iKrT/gw0MhwDAAAAAVVTREMAAAAAQj59BfLsr7/sGSshWj8b6WrtuNjnAlSr40E+AgfeVrIAAAAAAvrwgAAAAAAAAAABgRA+VQAAAECboQvzfHhL04WnLPMfhCJoeT8ozvEnhwuYbm8oQZP8CwLbKBvN+d+sm85TB/Eb8iYj5SoB4SKo6Gww44aLot0G",
              "payments": [
                {
                  "id": "3402095134773249",
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
          "memo": "3802820112",
          "memo_type": "id"
        }
      },
      {
        "type": "TRANSACTION_STATUS_CHANGED",
        "id": "670726c6-d975-418d-8897-44bb8dec8fc2",
        "sep": "24",
        "transaction": {
          "id": "24dcb15c-40a8-4636-ae5a-6fb76b80c1c1",
          "sep": "24",
          "kind": "withdrawal",
          "status": "completed",
          "amount_expected": {
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_in": {
            "amount": "5.0000000",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_out": {
            "amount": "4.5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "amount_fee": {
            "amount": "0.5",
            "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
          },
          "started_at": "2023-08-01T13:14:46.687761Z",
          "updated_at": "2023-08-01T13:15:15.294995Z",
          "message": "completed",
          "stellar_transactions": [
            {
              "id": "32969862db2b562832d331cd418b3ad96d82bd7f8dec8d7e684e86db489b6892",
              "memo": "3802820112",
              "memo_type": "id",
              "created_at": "2023-08-01T13:14:54Z",
              "envelope": "AAAAAgAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAGQAABUwAAAKYQAAAAEAAAAAAAAAAAAAAABkyQX9AAAAAgAAAADiqm4QAAAAAQAAAAAAAAABAAAAAEGxI2xgj2aqGDYg25rygTOTzyLGOpz9iKrT/gw0MhwDAAAAAVVTREMAAAAAQj59BfLsr7/sGSshWj8b6WrtuNjnAlSr40E+AgfeVrIAAAAAAvrwgAAAAAAAAAABgRA+VQAAAECboQvzfHhL04WnLPMfhCJoeT8ozvEnhwuYbm8oQZP8CwLbKBvN+d+sm85TB/Eb8iYj5SoB4SKo6Gww44aLot0G",
              "payments": [
                {
                  "id": "3402095134773249",
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
          "memo": "3802820112",
          "memo_type": "id"
        }
      }
    ]
  """
      .trimIndent()
}
