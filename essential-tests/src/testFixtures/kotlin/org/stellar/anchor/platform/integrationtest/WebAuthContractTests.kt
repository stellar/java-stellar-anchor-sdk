package org.stellar.anchor.platform.integrationtest

import kotlin.test.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.stellar.anchor.client.Sep10CClient
import org.stellar.anchor.platform.SMART_WALLET_ADDRESS
import org.stellar.anchor.platform.SMART_WALLET_SIGNER_SECRET
import org.stellar.anchor.platform.e2etest.WITHDRAW_FUND_CLIENT_SECRET_2
import org.stellar.anchor.util.Log
import org.stellar.sdk.*
import org.stellar.sdk.Transaction
import org.stellar.sdk.scval.Scv
import org.stellar.sdk.xdr.*

class WebAuthContractTests {
  private val sorobanServer = SorobanServer("https://soroban-testnet.stellar.org")
  private val network = Network("Test SDF Network ; September 2015")
  private val keypair = KeyPair.fromSecretSeed(SMART_WALLET_SIGNER_SECRET)
  private val account = sorobanServer.getAccount(keypair.accountId)
  private val webAuthContract = "CDQDXQPUUDLUZGSBQZBUMZA6ZKVR5JWEX4Y32K3MYQYUMAHIJFLVNOYB"

  private val anchorKeypair = KeyPair.fromSecretSeed(WITHDRAW_FUND_CLIENT_SECRET_2)
  private val anchorAccount = sorobanServer.getAccount(anchorKeypair.accountId)

  @Test
  fun testWebAuthVerify() {
    val parameters =
      mutableListOf(
        SCVal.Builder()
          .discriminant(SCValType.SCV_STRING)
          .str(Scv.toString(SMART_WALLET_ADDRESS).str)
          .build(),
        SCVal.Builder().discriminant(SCValType.SCV_VOID).build(), // memo is None
        SCVal.Builder()
          .discriminant(SCValType.SCV_STRING)
          .str(Scv.toString("http://home_domain.com:8080/").str)
          .build(),
        SCVal.Builder()
          .discriminant(SCValType.SCV_STRING)
          .str(Scv.toString("http://web_auth_domain.com:8080/").str)
          .build(),
        SCVal.Builder()
          .discriminant(SCValType.SCV_STRING)
          .str(Scv.toString("http://client_domain:8080/").str)
          .build(),
        SCVal.Builder()
          .discriminant(SCValType.SCV_STRING)
          .str(Scv.toString(SMART_WALLET_ADDRESS).str)
          .build(),
      )
    val operation =
      InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
          webAuthContract,
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
          webAuthContract,
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
