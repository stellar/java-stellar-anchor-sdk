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
import org.springframework.web.util.UriComponentsBuilder
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.api.sep.sep24.Sep24GetTransactionResponse
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.auth.Sep24InteractiveUrlJwt
import org.stellar.anchor.platform.CLIENT_WALLET_SECRET
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.util.GsonUtils
import org.stellar.anchor.util.Log.info
import org.stellar.anchor.util.StringHelper.json
import org.stellar.reference.client.AnchorReferenceServerClient
import org.stellar.reference.wallet.WalletServerClient
import org.stellar.walletsdk.ApplicationConfiguration
import org.stellar.walletsdk.InteractiveFlowResponse
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
  private val maxTries = 90
  private val anchorReferenceServerClient =
    AnchorReferenceServerClient(Url(config.env["reference.server.url"]!!))
  private val walletServerClient = WalletServerClient(Url(config.env["wallet.server.url"]!!))
  private val jwtService: JwtService =
    JwtService(
      config.env["secret.sep10.jwt_secret"]!!,
      config.env["secret.sep24.interactive_url.jwt_secret"]!!,
      config.env["secret.sep24.more_info_url.jwt_secret"]!!,
      config.env["secret.callback_api.auth_secret"]!!,
      config.env["secret.platform_api.auth_secret"]!!,
      config.env["secret.custody_server.auth_secret"]!!
    )

  private fun `test typical deposit end-to-end flow`(asset: StellarAssetId, amount: String) =
    runBlocking {
      walletServerClient.clearCallbacks()
      val token = anchor.auth().authenticate(keypair)
      val response = makeDeposit(asset, amount, token)

      // Assert the interactive URL JWT is valid
      val params = UriComponentsBuilder.fromUriString(response.url).build().queryParams
      val cipher = params["token"]!![0]
      val interactiveJwt = jwtService.decode(cipher, Sep24InteractiveUrlJwt::class.java)
      assertEquals("referenceCustodial", interactiveJwt.claims[JwtService.CLIENT_NAME])

      // Wait for the status to change to COMPLETED
      waitForTxnStatus(response.id, COMPLETED, token)

      // Check if the transaction can be listed by stellar transaction id
      val fetchedTxn = anchor.getTransaction(response.id, token) as DepositTransaction
      val transactionByStellarId =
        anchor.getTransactionBy(token, stellarTransactionId = fetchedTxn.stellarTransactionId)
      assertEquals(fetchedTxn.id, transactionByStellarId.id)

      // Check the events sent to the reference server are recorded correctly
      val actualEvents = waitForBusinessServerEvents(response.id, 5)
      assertNotNull(actualEvents)
      actualEvents?.let { assertEquals(5, it.size) }
      val expectedEvents: List<AnchorEvent> =
        gson.fromJson(expectedDepositEventsJson, object : TypeToken<List<AnchorEvent>>() {}.type)
      compareAndAssertEvents(asset, expectedEvents, actualEvents!!)

      // Check the callbacks sent to the wallet reference server are recorded correctly
      val actualCallbacks = waitForWalletServerCallbacks(response.id, 4)
      actualCallbacks?.let {
        assertEquals(4, it.size)
        val expectedCallbacks: List<Sep24GetTransactionResponse> =
          gson.fromJson(
            expectedDepositCallbacksJson,
            object : TypeToken<List<Sep24GetTransactionResponse>>() {}.type
          )
        compareAndAssertCallbacks(asset, expectedCallbacks, actualCallbacks)
      }
    }

  private suspend fun makeDeposit(
    asset: StellarAssetId,
    amount: String,
    token: AuthToken
  ): InteractiveFlowResponse {
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

    return deposit
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
        expectedEvent.transaction.completedAt = actualEvent.transaction.completedAt
        expectedEvent.transaction.stellarTransactions = actualEvent.transaction.stellarTransactions
        expectedEvent.transaction.memo = actualEvent.transaction.memo
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

  private fun compareAndAssertCallbacks(
    asset: StellarAssetId,
    expectedCallbacks: List<Sep24GetTransactionResponse>,
    actualCallbacks: List<Sep24GetTransactionResponse>
  ) {
    expectedCallbacks.forEachIndexed { index, expectedCallback ->
      actualCallbacks[index].let { actualCallback ->
        with(expectedCallback.transaction) {
          id = actualCallback.transaction.id
          moreInfoUrl = actualCallback.transaction.moreInfoUrl
          startedAt = actualCallback.transaction.startedAt
          to = actualCallback.transaction.to
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
    JSONAssert.assertEquals(json(expectedCallbacks), json(actualCallbacks), true)
  }

  private fun `test typical withdraw end-to-end flow`(asset: StellarAssetId, amount: String) {
    `test typical withdraw end-to-end flow`(asset, mapOf("amount" to amount))
  }

  private fun `test typical withdraw end-to-end flow`(
    asset: StellarAssetId,
    extraFields: Map<String, String>
  ) = runBlocking {
    walletServerClient.clearCallbacks()

    val token = anchor.auth().authenticate(keypair)
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
    waitForTxnStatus(withdrawTxn.id, PENDING_USER_TRANSFER_START, token)
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
    waitForTxnStatus(withdrawTxn.id, COMPLETED, token)

    // Check if the transaction can be listed by stellar transaction id
    val fetchTxn = anchor.getTransaction(withdrawTxn.id, token) as WithdrawalTransaction
    val transactionByStellarId =
      anchor.getTransactionBy(token, stellarTransactionId = fetchTxn.stellarTransactionId)
    assertEquals(fetchTxn.id, transactionByStellarId.id)

    // Check the events sent to the reference server are recorded correctly
    val actualEvents = waitForBusinessServerEvents(withdrawTxn.id, 5)
    assertNotNull(actualEvents)
    actualEvents?.let {
      assertEquals(5, it.size)
      val expectedEvents: List<AnchorEvent> =
        gson.fromJson(expectedWithdrawEventJson, object : TypeToken<List<AnchorEvent>>() {}.type)
      compareAndAssertEvents(asset, expectedEvents, actualEvents)
    }

    // Check the callbacks sent to the wallet reference server are recorded correctly
    val actualCallbacks = waitForWalletServerCallbacks(withdrawTxn.id, 5)
    actualCallbacks?.let {
      assertEquals(5, it.size)
      val expectedCallbacks: List<Sep24GetTransactionResponse> =
        gson.fromJson(
          expectedWithdrawalCallbacksJson,
          object : TypeToken<List<Sep24GetTransactionResponse>>() {}.type
        )
      compareAndAssertCallbacks(asset, expectedCallbacks, actualCallbacks)
    }
  }

  private suspend fun waitForWalletServerCallbacks(
    txnId: String,
    count: Int
  ): List<Sep24GetTransactionResponse>? {
    var retries = 5
    while (retries > 0) {
      val callbacks = walletServerClient.getCallbackHistory(txnId)
      if (callbacks.size == count) {
        return callbacks
      }
      delay(1.seconds)
      retries--
    }
    return null
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

  private suspend fun waitForTxnStatus(
    id: String,
    expectedStatus: TransactionStatus,
    token: AuthToken
  ) {
    var status: TransactionStatus? = null

    for (i in 0..maxTries) {
      // Get transaction info
      val transaction = anchor.getTransaction(id, token)

      if (status != transaction.status) {
        status = transaction.status

        info(
          "Transaction(id=${transaction.id}) status changed to $status. Message: ${transaction.message}"
        )
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
        val txnId = makeDeposit(asset, amount, token).id
        waitForTxnStatus(txnId, COMPLETED, token)
        txnId
      }
    val history = anchor.getHistory(asset, token)

    Assertions.assertThat(history).allMatch { deposits.contains(it.id) }
  }

  fun testAll() {
    info("Running SEP-24 USDC end-to-end tests...")
    `test typical deposit end-to-end flow`(USDC, "1.1")
    `test typical withdraw end-to-end flow`(USDC, "1.1")
    `test created transactions show up in the get history call`(USDC, "1.1")
    info("Running SEP-24 XLM end-to-end tests...")
    `test typical deposit end-to-end flow`(XLM, "0.00001")
    `test typical withdraw end-to-end flow`(XLM, "0.00001")
    `test created transactions show up in the get history call`(XLM, "0.00001")
  }

  companion object {
    private val USDC =
      IssuedAssetId("USDC", "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5")

    val expectedDepositEventsJson =
      """
[
  {
    "type": "transaction_created",
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
    "type": "transaction_status_changed",
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
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "type": "transaction_status_changed",
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
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "type": "transaction_status_changed",
    "sep": "24",
    "transaction": {
      "sep": "24",
      "kind": "deposit",
      "status": "pending_stellar",
      "amount_expected": {
      },
      "amount_in": {
      },
      "amount_out": {
      },
      "amount_fee": {
      },
      "message": "funds received, transaction is being processed",
      "destination_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "type": "transaction_status_changed",
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
      "message": "Outgoing payment sent",
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
    "type": "transaction_created",
    "id": "a32464b5-e4f2-4d85-955a-2f09696b3103",
    "sep": "24",
    "transaction": {
      "id": "419d2cd3-1ebd-42fc-80c5-94fe506d144b",
      "sep": "24",
      "kind": "withdrawal",
      "status": "incomplete",
      "amount_expected": {
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-30T09:48:08.715329Z",
      "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "destination_account": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF"
    }
  },
  {
    "type": "transaction_status_changed",
    "id": "0f454aa9-e328-4967-befe-b686410f6e54",
    "sep": "24",
    "transaction": {
      "id": "419d2cd3-1ebd-42fc-80c5-94fe506d144b",
      "sep": "24",
      "kind": "withdrawal",
      "status": "pending_user_transfer_start",
      "amount_expected": {
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "1.1",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "1.0",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "0.1",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-30T09:48:08.715329Z",
      "updated_at": "2023-08-30T09:48:09.788551Z",
      "message": "waiting on the user to transfer funds",
      "source_account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "destination_account": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6",
      "memo": "2653020630",
      "memo_type": "id"
    }
  },
  {
    "type": "transaction_status_changed",
    "id": "e8b7a043-7952-4803-8886-34c5fefa459a",
    "sep": "24",
    "transaction": {
      "id": "419d2cd3-1ebd-42fc-80c5-94fe506d144b",
      "sep": "24",
      "kind": "withdrawal",
      "status": "pending_anchor",
      "amount_expected": {
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "1.1000000",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "1.0",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "0.1",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-30T09:48:08.715329Z",
      "updated_at": "2023-08-30T09:48:20.560738Z",
      "message": "Received an incoming payment",
      "stellar_transactions": [
        {
          "id": "979b540a85c84854980ed8378f333b470071ef34731e998a43a1272e0377c6b9",
          "memo": "2653020630",
          "memo_type": "id",
          "created_at": "2023-08-30T09:48:13Z",
          "envelope": "AAAAAgAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAGQAABUwAAASdQAAAAEAAAAAAAAAAAAAAABk7xEQAAAAAgAAAACeIeHWAAAAAQAAAAAAAAABAAAAAEGxI2xgj2aqGDYg25rygTOTzyLGOpz9iKrT/gw0MhwDAAAAAVVTREMAAAAAQj59BfLsr7/sGSshWj8b6WrtuNjnAlSr40E+AgfeVrIAAAAAAKfYwAAAAAAAAAABgRA+VQAAAECf6CEOtlF882R4A650Op2kf9MwYc7E9TrQdCyzU54Dr1dZHiA3h/Yf8/51En5MIngAs0ZkIiIwIy1zD3P0KxMO",
          "payments": [
            {
              "id": "5439412871634945",
              "amount": {
                "amount": "1.1000000",
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
      "memo": "2653020630",
      "memo_type": "id"
    }
  },
  {
    "type": "transaction_status_changed",
    "id": "91da8107-2916-44e5-a34b-5e533b31b8b6",
    "sep": "24",
    "transaction": {
      "id": "419d2cd3-1ebd-42fc-80c5-94fe506d144b",
      "sep": "24",
      "kind": "withdrawal",
      "status": "pending_external",
      "amount_expected": {
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "1.1000000",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "1.0",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "0.1",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-30T09:48:08.715329Z",
      "updated_at": "2023-08-30T09:48:26.429049Z",
      "message": "pending external transfer",
      "stellar_transactions": [
        {
          "id": "979b540a85c84854980ed8378f333b470071ef34731e998a43a1272e0377c6b9",
          "memo": "2653020630",
          "memo_type": "id",
          "created_at": "2023-08-30T09:48:13Z",
          "envelope": "AAAAAgAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAGQAABUwAAASdQAAAAEAAAAAAAAAAAAAAABk7xEQAAAAAgAAAACeIeHWAAAAAQAAAAAAAAABAAAAAEGxI2xgj2aqGDYg25rygTOTzyLGOpz9iKrT/gw0MhwDAAAAAVVTREMAAAAAQj59BfLsr7/sGSshWj8b6WrtuNjnAlSr40E+AgfeVrIAAAAAAKfYwAAAAAAAAAABgRA+VQAAAECf6CEOtlF882R4A650Op2kf9MwYc7E9TrQdCyzU54Dr1dZHiA3h/Yf8/51En5MIngAs0ZkIiIwIy1zD3P0KxMO",
          "payments": [
            {
              "id": "5439412871634945",
              "amount": {
                "amount": "1.1000000",
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
      "memo": "2653020630",
      "memo_type": "id"
    }
  },
  {
    "type": "transaction_status_changed",
    "id": "ed653e12-e4ad-4cec-b71f-571855dfa1bb",
    "sep": "24",
    "transaction": {
      "id": "419d2cd3-1ebd-42fc-80c5-94fe506d144b",
      "sep": "24",
      "kind": "withdrawal",
      "status": "completed",
      "amount_expected": {
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_in": {
        "amount": "1.1000000",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_out": {
        "amount": "1.0",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "amount_fee": {
        "amount": "0.1",
        "asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
      },
      "started_at": "2023-08-30T09:48:08.715329Z",
      "updated_at": "2023-08-30T09:48:27.467368Z",
      "message": "completed",
      "stellar_transactions": [
        {
          "id": "979b540a85c84854980ed8378f333b470071ef34731e998a43a1272e0377c6b9",
          "memo": "2653020630",
          "memo_type": "id",
          "created_at": "2023-08-30T09:48:13Z",
          "envelope": "AAAAAgAAAADSsOMKYK7a1aALie83F4GQDoBdHrW86UX2SYVygRA+VQAAAGQAABUwAAASdQAAAAEAAAAAAAAAAAAAAABk7xEQAAAAAgAAAACeIeHWAAAAAQAAAAAAAAABAAAAAEGxI2xgj2aqGDYg25rygTOTzyLGOpz9iKrT/gw0MhwDAAAAAVVTREMAAAAAQj59BfLsr7/sGSshWj8b6WrtuNjnAlSr40E+AgfeVrIAAAAAAKfYwAAAAAAAAAABgRA+VQAAAECf6CEOtlF882R4A650Op2kf9MwYc7E9TrQdCyzU54Dr1dZHiA3h/Yf8/51En5MIngAs0ZkIiIwIy1zD3P0KxMO",
          "payments": [
            {
              "id": "5439412871634945",
              "amount": {
                "amount": "1.1000000",
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
      "memo": "2653020630",
      "memo_type": "id"
    }
  }
]
  """
      .trimIndent()

  private val expectedWithdrawalCallbacksJson =
    """
[
  {
    "transaction": {
      "id": "c849c9b4-b9cd-4659-b574-fdc1d8fb7e3b",
      "kind": "withdrawal",
      "status": "incomplete",
      "more_info_url": "http://localhost:8080/sep24/transaction/more_info?transaction_id\u003dc849c9b4-b9cd-4659-b574-fdc1d8fb7e3b\u0026token\u003deyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjODQ5YzliNC1iOWNkLTQ2NTktYjU3NC1mZGMxZDhmYjdlM2IiLCJleHAiOjE2OTEwNTUwNDUsInN1YiI6IkdESkxCWVlLTUNYTlZWTkFCT0U2Nk5ZWFFHSUE1QUM1RDIyM1oyS0Y2WkVZSzRVQkNBN0ZLTFRHIiwiZGF0YSI6e319.7uwSjsxMy5DKNtiSrncEe1Sugnhs7m2ALm1_1jJZ_Ac",
      "started_at": "2023-08-03T09:20:44.557598Z",
      "refunded": false,
      "from": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "to": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6"
    }
  },
  {
    "transaction": {
      "id": "c849c9b4-b9cd-4659-b574-fdc1d8fb7e3b",
      "kind": "withdrawal",
      "status": "pending_user_transfer_start",
      "more_info_url": "http://localhost:8080/sep24/transaction/more_info?transaction_id\u003dc849c9b4-b9cd-4659-b574-fdc1d8fb7e3b\u0026token\u003deyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjODQ5YzliNC1iOWNkLTQ2NTktYjU3NC1mZGMxZDhmYjdlM2IiLCJleHAiOjE2OTEwNTUwNDYsInN1YiI6IkdESkxCWVlLTUNYTlZWTkFCT0U2Nk5ZWFFHSUE1QUM1RDIyM1oyS0Y2WkVZSzRVQkNBN0ZLTFRHIiwiZGF0YSI6e319.kjMVXWto256FX4JaT8zxIhFOSN9J3RU5j5jfhqAXL_4",
      "amount_in": "5",
      "amount_in_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "amount_out": "4.5",
      "amount_out_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "amount_fee": "0.5",
      "amount_fee_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "started_at": "2023-08-03T09:20:44.557598Z",
      "message": "waiting on the user to transfer funds",
      "refunded": false,
      "from": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "to": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6"
    }
  },
  {
    "transaction": {
      "id": "c849c9b4-b9cd-4659-b574-fdc1d8fb7e3b",
      "kind": "withdrawal",
      "status": "pending_anchor",
      "more_info_url": "http://localhost:8080/sep24/transaction/more_info?transaction_id\u003dc849c9b4-b9cd-4659-b574-fdc1d8fb7e3b\u0026token\u003deyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjODQ5YzliNC1iOWNkLTQ2NTktYjU3NC1mZGMxZDhmYjdlM2IiLCJleHAiOjE2OTEwNTUwNTgsInN1YiI6IkdESkxCWVlLTUNYTlZWTkFCT0U2Nk5ZWFFHSUE1QUM1RDIyM1oyS0Y2WkVZSzRVQkNBN0ZLTFRHIiwiZGF0YSI6e319.P1dUodT6b-WeOgiJbdNdGeM-wLExbPU1olPH6CMwLaE",
      "amount_in": "5",
      "amount_in_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "amount_out": "4.5",
      "amount_out_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "amount_fee": "0.5",
      "amount_fee_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "started_at": "2023-08-03T09:20:44.557598Z",
      "stellar_transaction_id": "a2d31bbed336393dda0e00c09e37cd141cf5d17b7bb780c19a204bd3976e3aa7",
      "message": "waiting on the user to transfer funds",
      "refunded": false,
      "from": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "to": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6"
    }
  },
  {
    "transaction": {
      "id": "c849c9b4-b9cd-4659-b574-fdc1d8fb7e3b",
      "kind": "withdrawal",
      "status": "pending_external",
      "more_info_url": "http://localhost:8080/sep24/transaction/more_info?transaction_id\u003dc849c9b4-b9cd-4659-b574-fdc1d8fb7e3b\u0026token\u003deyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjODQ5YzliNC1iOWNkLTQ2NTktYjU3NC1mZGMxZDhmYjdlM2IiLCJleHAiOjE2OTEwNTUwNjIsInN1YiI6IkdESkxCWVlLTUNYTlZWTkFCT0U2Nk5ZWFFHSUE1QUM1RDIyM1oyS0Y2WkVZSzRVQkNBN0ZLTFRHIiwiZGF0YSI6e319.pkolr6408DXi-SiJNy28KfRPVt2rx30FcSKxGohLY6Y",
      "amount_in": "5",
      "amount_in_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "amount_out": "4.5",
      "amount_out_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "amount_fee": "0.5",
      "amount_fee_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "started_at": "2023-08-03T09:20:44.557598Z",
      "stellar_transaction_id": "a2d31bbed336393dda0e00c09e37cd141cf5d17b7bb780c19a204bd3976e3aa7",
      "message": "pending external transfer",
      "refunded": false,
      "from": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "to": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6"
    }
  },
  {
    "transaction": {
      "id": "c849c9b4-b9cd-4659-b574-fdc1d8fb7e3b",
      "kind": "withdrawal",
      "status": "completed",
      "more_info_url": "http://localhost:8080/sep24/transaction/more_info?transaction_id\u003dc849c9b4-b9cd-4659-b574-fdc1d8fb7e3b\u0026token\u003deyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJjODQ5YzliNC1iOWNkLTQ2NTktYjU3NC1mZGMxZDhmYjdlM2IiLCJleHAiOjE2OTEwNTUwNjMsInN1YiI6IkdESkxCWVlLTUNYTlZWTkFCT0U2Nk5ZWFFHSUE1QUM1RDIyM1oyS0Y2WkVZSzRVQkNBN0ZLTFRHIiwiZGF0YSI6e319.Ga2LcUgRPPJYyJrlEd0r5gDvLwP3YEMb3KsN-TD6Wg8",
      "amount_in": "5",
      "amount_in_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "amount_out": "4.5",
      "amount_out_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "amount_fee": "0.5",
      "amount_fee_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "started_at": "2023-08-03T09:20:44.557598Z",
      "stellar_transaction_id": "a2d31bbed336393dda0e00c09e37cd141cf5d17b7bb780c19a204bd3976e3aa7",
      "message": "Completed",
      "refunded": false,
      "from": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
      "to": "GBA3CI3MMCHWNKQYGYQNXGXSQEZZHTZCYY5JZ7MIVLJ74DBUGIOAGNV6"
    }
  }
]    
  """
      .trimIndent()

  private val expectedDepositCallbacksJson =
    """
[
  {
    "transaction": {
      "id": "96166ee5-2bf1-4a44-a509-d074e505f4b3",
      "kind": "deposit",
      "status": "incomplete",
      "more_info_url": "http://localhost:8080/sep24/transaction/more_info?transaction_id\u003d96166ee5-2bf1-4a44-a509-d074e505f4b3\u0026token\u003deyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5NjE2NmVlNS0yYmYxLTRhNDQtYTUwOS1kMDc0ZTUwNWY0YjMiLCJleHAiOjE2OTEwNTUwMDcsInN1YiI6IkdESkxCWVlLTUNYTlZWTkFCT0U2Nk5ZWFFHSUE1QUM1RDIyM1oyS0Y2WkVZSzRVQkNBN0ZLTFRHIiwiZGF0YSI6e319.mW2DHAk8wZyvTY2SM8Wzt2hpkkefWdq7RIn-SGwfk6I",
      "started_at": "2023-08-03T09:20:06.732254Z",
      "refunded": false,
      "to": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "transaction": {
      "id": "96166ee5-2bf1-4a44-a509-d074e505f4b3",
      "kind": "deposit",
      "status": "pending_user_transfer_start",
      "more_info_url": "http://localhost:8080/sep24/transaction/more_info?transaction_id\u003d96166ee5-2bf1-4a44-a509-d074e505f4b3\u0026token\u003deyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5NjE2NmVlNS0yYmYxLTRhNDQtYTUwOS1kMDc0ZTUwNWY0YjMiLCJleHAiOjE2OTEwNTUwMDgsInN1YiI6IkdESkxCWVlLTUNYTlZWTkFCT0U2Nk5ZWFFHSUE1QUM1RDIyM1oyS0Y2WkVZSzRVQkNBN0ZLTFRHIiwiZGF0YSI6e319.a1o-Qw1XRy3xr84LPPf4VTJWT_NJpz9Qw-Po5Qy9GW4",
      "amount_in": "5",
      "amount_in_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "amount_out": "4.5",
      "amount_out_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "amount_fee": "0.5",
      "amount_fee_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "started_at": "2023-08-03T09:20:06.732254Z",
      "message": "waiting on the user to transfer funds",
      "refunded": false,
      "to": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "transaction": {
      "id": "96166ee5-2bf1-4a44-a509-d074e505f4b3",
      "kind": "deposit",
      "status": "pending_anchor",
      "more_info_url": "http://localhost:8080/sep24/transaction/more_info?transaction_id\u003d96166ee5-2bf1-4a44-a509-d074e505f4b3\u0026token\u003deyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5NjE2NmVlNS0yYmYxLTRhNDQtYTUwOS1kMDc0ZTUwNWY0YjMiLCJleHAiOjE2OTEwNTUwMDksInN1YiI6IkdESkxCWVlLTUNYTlZWTkFCT0U2Nk5ZWFFHSUE1QUM1RDIyM1oyS0Y2WkVZSzRVQkNBN0ZLTFRHIiwiZGF0YSI6e319.xSaWtb4Eci_eF1CT1qD3BsYgGdTwKfSDGqoANOMbWBo",
      "amount_in": "5",
      "amount_in_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "amount_out": "4.5",
      "amount_out_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "amount_fee": "0.5",
      "amount_fee_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "started_at": "2023-08-03T09:20:06.732254Z",
      "message": "funds received, transaction is being processed",
      "refunded": false,
      "to": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  },
  {
    "transaction": {
      "id": "96166ee5-2bf1-4a44-a509-d074e505f4b3",
      "kind": "deposit",
      "status": "completed",
      "more_info_url": "http://localhost:8080/sep24/transaction/more_info?transaction_id\u003d96166ee5-2bf1-4a44-a509-d074e505f4b3\u0026token\u003deyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI5NjE2NmVlNS0yYmYxLTRhNDQtYTUwOS1kMDc0ZTUwNWY0YjMiLCJleHAiOjE2OTEwNTUwMTYsInN1YiI6IkdESkxCWVlLTUNYTlZWTkFCT0U2Nk5ZWFFHSUE1QUM1RDIyM1oyS0Y2WkVZSzRVQkNBN0ZLTFRHIiwiZGF0YSI6e319.tlN5RkzLTLbcWdgPRkpLO1GAArc4xQ3LBizU1t4ZE9Y",
      "amount_in": "5",
      "amount_in_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "amount_out": "4.5",
      "amount_out_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "amount_fee": "0.5",
      "amount_fee_asset": "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      "started_at": "2023-08-03T09:20:06.732254Z",
      "stellar_transaction_id": "089c75468a7927a43e5e429b7f0a2e8e0960761b2ce98fd4907998017d343bc9",
      "message": "Outgoing payment sent",
      "refunded": false,
      "to": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  }
]
  """
      .trimIndent()
}
