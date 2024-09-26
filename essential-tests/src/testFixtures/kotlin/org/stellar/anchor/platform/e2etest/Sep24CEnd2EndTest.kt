package org.stellar.anchor.platform.e2etest

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import java.math.BigInteger
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.sep.sep10c.ChallengeRequest
import org.stellar.anchor.client.Sep10CClient
import org.stellar.anchor.client.Sep24Client
import org.stellar.anchor.platform.AbstractIntegrationTests
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.util.Log.info
import org.stellar.sdk.*
import org.stellar.sdk.scval.Scv
import org.stellar.sdk.xdr.HashIDPreimage
import org.stellar.sdk.xdr.SCVal
import org.stellar.sdk.xdr.SCValType
import org.stellar.sdk.xdr.SorobanAuthorizationEntry

class Sep24CEnd2EndTest : AbstractIntegrationTests(TestConfig()) {
  private val client = HttpClient {
    install(HttpTimeout) {
      requestTimeoutMillis = 300000
      connectTimeoutMillis = 300000
      socketTimeoutMillis = 300000
    }
  }
  private var sep10CClient: Sep10CClient =
    Sep10CClient(
      toml.getString("WEB_AUTH_ENDPOINT_C"),
      toml.getString("SIGNING_KEY"),
      SorobanServer("https://soroban-testnet.stellar.org"),
    )
  private var sep24Client: Sep24Client
  private val webAuthDomain = toml.getString("WEB_AUTH_ENDPOINT_C")
  private val clientWalletContractAddress =
    "CDYOQJLKZWHZ2CVN43EVEQNDLEN544IGCO5A52UG4YS6KDN5QQ2LUWKY"

  private val sorobanServer: SorobanServer = SorobanServer("https://soroban-testnet.stellar.org")
  private val network = Network("Test SDF Network ; September 2015")
  private val walletContract = "CDYOQJLKZWHZ2CVN43EVEQNDLEN544IGCO5A52UG4YS6KDN5QQ2LUWKY"
  private val USDCContract = "CDTY3P6OVY3SMZXR3DZA667NAXFECA6A3AOZXEU33DD2ACBY43CIKDPT"

  private val philip = "SCAWZ3DBU5UVT3SLDMLNPP4GUDP7WDCOYQDE5JHCTXHLS3TAJIZ4HJOC"
  private val keyPair = KeyPair.fromSecretSeed(philip)

  init {
    val challenge =
      sep10CClient.getChallenge(
        ChallengeRequest.builder()
          .address(clientWalletContractAddress)
          .homeDomain(webAuthDomain)
          .build()
      )
    val validationRequest = sep10CClient.sign(challenge)
    val token = sep10CClient.validate(validationRequest).token

    sep24Client = Sep24Client(toml.getString("TRANSFER_SERVER_SEP0024"), token)
  }

  @Test
  fun testWithdrawFullFlow() = runBlocking {
    val request =
      mapOf(
        "amount" to "1",
        "asset_code" to "USDC",
        "asset_issuer" to "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
        "account" to clientWalletContractAddress,
        "lang" to "en",
      )
    val response = sep24Client.withdraw(request)
    val transaction = sep24Client.getTransaction(response.id, "USDC").transaction
    assertEquals("incomplete", transaction.status)

    // Start the withdrawal process
    val interactiveUrlRes = client.get(response.url)
    assertEquals(200, interactiveUrlRes.status.value)

    // Transfer funds
    waitForTxnStatus(transaction.id, "pending_user_transfer_start")
    transferFunds(transaction.to, walletContract, 1, keyPair)

    // Wait for the transaction to complete
    waitForTxnStatus(transaction.id, "completed")
  }

  suspend fun waitForTxnStatus(id: String, expectedStatus: String, exitStatus: String = "error") {
    var status: String? = null

    for (i in 0..30) {
      // Get transaction info
      val transaction = sep24Client.getTransaction(id, "USDC").transaction
      if (status != transaction.status) {
        status = transaction.status
        info(
          "Transaction(id=${transaction.id}) status changed to $status. Message: ${transaction.message}"
        )
      }

      if (transaction.status == expectedStatus) return

      if (transaction.status == exitStatus) break

      delay(1.seconds)
    }

    fail("Transaction wasn't $expectedStatus in 30 tries, last status: $status")
  }

  private fun transferFunds(to: String, from: String, amount: Long, keypair: KeyPair) {
    val parameters =
      mutableListOf(
        // from=
        SCVal.Builder()
          .discriminant(SCValType.SCV_ADDRESS)
          .address(Scv.toAddress(from).address)
          .build(),
        // to=
        SCVal.Builder()
          .discriminant(SCValType.SCV_ADDRESS)
          .address(Scv.toAddress(to).address)
          .build(),
        // amount=
        SCVal.Builder()
          .discriminant(SCValType.SCV_I128)
          .i128(Scv.toInt128(BigInteger.valueOf(amount * 10000000)).i128)
          .build(),
      )
    val operation =
      InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
          USDCContract,
          "transfer",
          parameters,
        )
        .sourceAccount(keypair.accountId)
        .build()

    var account = sorobanServer.getAccount(keypair.accountId)
    val transaction =
      TransactionBuilder(account, network)
        .addOperation(operation)
        .setBaseFee(Transaction.MIN_BASE_FEE.toLong())
        .setTimeout(300)
        .build()

    val simulationResponse = sorobanServer.simulateTransaction(transaction)
    val signedAuthEntries = mutableListOf<SorobanAuthorizationEntry>()
    simulationResponse.results.forEach {
      it.auth.forEach { entryXdr ->
        val entry = SorobanAuthorizationEntry.fromXdrBase64(entryXdr)
        info("Unsigned auth entry: ${entry.toXdrBase64()}")
        val validUntilLedgerSeq = simulationResponse.latestLedger + 10

        val signer =
          object : Sep10CClient.Companion.Signer {
            override fun sign(preimage: HashIDPreimage): ByteArray {
              return keypair.sign(Util.hash(preimage.toXdrByteArray()))
            }

            override fun publicKey(): ByteArray {
              return keypair.publicKey
            }
          }

        val signedEntry = Sep10CClient.authorizeEntry(entry, signer, validUntilLedgerSeq, network)
        signedAuthEntries.add(signedEntry)
        info("Signed auth entry: ${signedEntry.toXdrBase64()}")
      }
    }

    val signedOperation =
      InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
          USDCContract,
          "transfer",
          parameters,
        )
        .sourceAccount(account.accountId)
        .auth(signedAuthEntries)
        .build()

    account = sorobanServer.getAccount(keypair.accountId)
    val authorizedTransaction =
      TransactionBuilder(account, network)
        .addOperation(signedOperation)
        .setBaseFee(Transaction.MIN_BASE_FEE.toLong())
        .setTimeout(300)
        .build()

    val preparedTransaction = sorobanServer.prepareTransaction(authorizedTransaction)
    preparedTransaction.sign(keypair)

    val transactionResponse = sorobanServer.sendTransaction(preparedTransaction)
    info("Transaction response: $transactionResponse")
  }
}
