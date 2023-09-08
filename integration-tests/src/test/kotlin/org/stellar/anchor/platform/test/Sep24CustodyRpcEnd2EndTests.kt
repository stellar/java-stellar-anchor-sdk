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
import org.stellar.anchor.api.callback.SendEventRequest
import org.stellar.anchor.api.callback.SendEventRequestPayload
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.api.event.AnchorEvent.Type.*
import org.stellar.anchor.api.sep.SepTransactionStatus
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
      assertEvents(actualEvents, expectedStatuses)

      // Check the callbacks sent to the wallet reference server are recorded correctly
      val actualCallbacks = waitForWalletServerCallbacks(response.id, 5)
      actualCallbacks?.let {
        assertEquals(5, it.size)
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

  private fun assertEvents(
    actualEvents: List<SendEventRequest>?,
    expectedStatuses: List<Pair<AnchorEvent.Type, SepTransactionStatus>>
  ) {
    assertNotNull(actualEvents)
    actualEvents?.let {
      assertEquals(expectedStatuses.size, actualEvents.size)

      expectedStatuses.forEachIndexed { index, expectedStatus ->
        actualEvents[index].let { actualEvent ->
          assertNotNull(actualEvent.id)
          assertNotNull(actualEvent.timestamp)
          assertEquals(expectedStatus.first.type, actualEvent.type)
          org.junit.jupiter.api.Assertions.assertTrue(
            actualEvent.payload is SendEventRequestPayload
          )
          assertEquals(expectedStatus.second, actualEvent.payload.transaction.status)
        }
      }
    }
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
    val actualEvents = waitForBusinessServerEvents(withdrawTxn.id, 4)
    assertEvents(actualEvents, expectedStatuses)

    // Check the callbacks sent to the wallet reference server are recorded correctly
    val actualCallbacks = waitForWalletServerCallbacks(withdrawTxn.id, 4)
    actualCallbacks?.let {
      assertEquals(4, it.size)
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
    var callbacks: List<Sep24GetTransactionResponse>? = null
    while (retries > 0) {
      callbacks =
        walletServerClient.getCallbackHistory(txnId, Sep24GetTransactionResponse::class.java)
      if (callbacks.size == count) {
        return callbacks
      }
      delay(1.seconds)
      retries--
    }
    return callbacks
  }

  private suspend fun waitForBusinessServerEvents(
    txnId: String,
    count: Int
  ): List<SendEventRequest>? {
    var retries = 5
    var events: List<SendEventRequest>? = null
    while (retries > 0) {
      events = anchorReferenceServerClient.getEvents(txnId)
      if (events.size == count) {
        return events
      }
      delay(1.seconds)
      retries--
    }
    return events
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
    private val expectedStatuses =
      listOf(
        TRANSACTION_CREATED to SepTransactionStatus.INCOMPLETE,
        TRANSACTION_STATUS_CHANGED to SepTransactionStatus.PENDING_USR_TRANSFER_START,
        TRANSACTION_STATUS_CHANGED to SepTransactionStatus.PENDING_ANCHOR,
        TRANSACTION_STATUS_CHANGED to SepTransactionStatus.PENDING_STELLAR,
        TRANSACTION_STATUS_CHANGED to SepTransactionStatus.COMPLETED
      )
  }

  private val expectedWithdrawalCallbacksJson =
    """
[
]    
  """
      .trimIndent()

  private val expectedDepositCallbacksJson =
    """
[
]
  """
      .trimIndent()
}
