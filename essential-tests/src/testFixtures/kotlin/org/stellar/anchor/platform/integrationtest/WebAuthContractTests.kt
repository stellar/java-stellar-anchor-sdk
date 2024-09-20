package org.stellar.anchor.platform.integrationtest

import kotlin.test.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.stellar.anchor.client.Sep10CClient
import org.stellar.anchor.platform.e2etest.WITHDRAW_FUND_CLIENT_SECRET_2
import org.stellar.anchor.util.Log
import org.stellar.sdk.*
import org.stellar.sdk.Transaction
import org.stellar.sdk.scval.Scv
import org.stellar.sdk.xdr.*

class WebAuthContractTests {
  private val sorobanServer = SorobanServer("https://soroban-testnet.stellar.org")
  private val network = Network("Test SDF Network ; September 2015")
  private val philip = "SCAWZ3DBU5UVT3SLDMLNPP4GUDP7WDCOYQDE5JHCTXHLS3TAJIZ4HJOC"
  private val someone = "SB75HOQAKNX3I6BR6W3VGZT6T26C5UDC2OISIIPK5SODKLPRLPD5VVRM"
  private val secret = philip
  private val keypair = KeyPair.fromSecretSeed(secret)
  private val account = sorobanServer.getAccount(keypair.accountId)
  private val contractAddress = "CC3HX7UUTL43JXMVOM2SVVCWNBWA4ZTIBJ5WWJ5SGI7EILKQ3OKEINSK"

  private val anchorKeypair = KeyPair.fromSecretSeed(WITHDRAW_FUND_CLIENT_SECRET_2)
  private val anchorAccount = sorobanServer.getAccount(anchorKeypair.accountId)

  /** Some initialization code to add my account as a signer to the contract account. */
  @Disabled
  @Test
  fun addClientAccountAsSigner() {
    val parameters =
      mutableListOf(
        SCVal.Builder()
          .discriminant(SCValType.SCV_VEC)
          .vec(
            SCVec(
              arrayOf(
                SCVal.Builder()
                  .discriminant(SCValType.SCV_BYTES)
                  .bytes(SCBytes(keypair.publicKey))
                  .build()
              )
            )
          )
          .build()
      )
    val operation =
      InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
          contractAddress,
          "init",
          parameters,
        )
        .sourceAccount(keypair.accountId)
        .build()

    val transaction =
      TransactionBuilder(account, network)
        .setBaseFee(Transaction.MIN_BASE_FEE.toLong())
        .addOperation(operation)
        .setTimeout(300)
        .build()

    val preparedTransaction = sorobanServer.prepareTransaction(transaction)
    preparedTransaction.sign(keypair)
    val result = sorobanServer.sendTransaction(preparedTransaction)
  }

  @Test
  fun testWebAuthVerify() {
    val parameters =
      mutableListOf(
        SCVal.Builder()
          .discriminant(SCValType.SCV_MAP)
          .map(
            SCMap(
              arrayOf(
                SCMapEntry.Builder()
                  .key(Scv.toString("account"))
                  .`val`(Scv.toString(contractAddress))
                  .build(),
                SCMapEntry.Builder()
                  .key(Scv.toString("web_auth_domain"))
                  .`val`(Scv.toString("localhost:8080"))
                  .build(),
              )
            )
          )
          .build()
      )
    val operation =
      InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
          contractAddress,
          "web_auth_verify",
          parameters,
        )
        .sourceAccount(anchorKeypair.accountId)
        .build()
    val transaction =
      TransactionBuilder(account, Network("Test SDF Network ; September 2015"))
        .setBaseFee(Transaction.MIN_BASE_FEE.toLong())
        .addOperation(operation)
        .setTimeout(300)
        .build()

    Log.info("Simulating transaction to get auth entries")
    val simulationResponse = sorobanServer.simulateTransaction(transaction)
    val signedAuthEntries = mutableListOf<SorobanAuthorizationEntry>()
    simulationResponse.results.forEach {
      it.auth.forEach { entryXdr ->
        val entry = SorobanAuthorizationEntry.fromXdrBase64(entryXdr)
        Log.info("Unsigned auth entry: ${entry.toXdrBase64()}")
        val validUntilLedgerSeq = simulationResponse.latestLedger + 100

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
        Log.info("Signed auth entry: ${signedEntry.toXdrBase64()}")
      }
    }

    Log.info("Attaching signed auth entries")
    val signedOperation =
      InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
          contractAddress,
          "web_auth_verify",
          parameters,
        )
        .sourceAccount(anchorKeypair.accountId)
        .auth(signedAuthEntries)
        .build()

    val authorizedTransaction =
      TransactionBuilder(anchorAccount, Network("Test SDF Network ; September 2015"))
        .setBaseFee(Transaction.MIN_BASE_FEE.toLong())
        .addOperation(signedOperation)
        .setTimeout(300)
        .build()

    val authSimulation = sorobanServer.simulateTransaction(authorizedTransaction)
    Log.info("Authorized transaction: ${authorizedTransaction.toEnvelopeXdrBase64()}")
    Log.info("Auth simulation result: $authSimulation")
    assertTrue(authSimulation.error == null)
  }
}
