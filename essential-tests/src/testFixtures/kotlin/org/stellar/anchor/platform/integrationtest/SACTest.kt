package org.stellar.anchor.platform.integrationtest

import java.math.BigInteger
import kotlin.test.Test
import org.stellar.anchor.platform.e2etest.WITHDRAW_FUND_CLIENT_SECRET_1
import org.stellar.sdk.*
import org.stellar.sdk.scval.Scv
import org.stellar.sdk.xdr.SCVal
import org.stellar.sdk.xdr.SCValType

class SACTest {
  private val sorobanServer: SorobanServer = SorobanServer("https://soroban-testnet.stellar.org")
  private val network = Network("Test SDF Network ; September 2015")
  private val walletContract = "CDYOQJLKZWHZ2CVN43EVEQNDLEN544IGCO5A52UG4YS6KDN5QQ2LUWKY"
  private val USDCContract = "CDTY3P6OVY3SMZXR3DZA667NAXFECA6A3AOZXEU33DD2ACBY43CIKDPT"

  private val anchorKeyPair = KeyPair.fromSecretSeed(WITHDRAW_FUND_CLIENT_SECRET_1)
  private val anchorAccount = sorobanServer.getAccount(anchorKeyPair.accountId)

  @Test
  fun testEnvelope() {
    val txn =
      sorobanServer.getTransaction(
        "8d86a04c7bf3583ee811f4547218dc83f2c56ce68d3c08a63c223ed3dd93119c"
      )
    println("Transaction: $txn")
  }

  @Test
  fun testSACTransfer() {
    val parameters =
      mutableListOf(
        // from=
        SCVal.Builder()
          .discriminant(SCValType.SCV_ADDRESS)
          .address(Scv.toAddress(anchorAccount.accountId).address)
          .build(),
        // to=
        SCVal.Builder()
          .discriminant(SCValType.SCV_ADDRESS)
          .address(Scv.toAddress(walletContract).address)
          .build(),
        // amount=
        SCVal.Builder()
          .discriminant(SCValType.SCV_I128)
          .i128(Scv.toInt128(BigInteger.ONE).i128)
          .build(),
      )
    val operation =
      InvokeHostFunctionOperation.invokeContractFunctionOperationBuilder(
          USDCContract,
          "transfer",
          parameters,
        )
        .sourceAccount(anchorKeyPair.accountId)
        .build()
    val transaction =
      TransactionBuilder(anchorAccount, network)
        .addOperation(operation)
        .setBaseFee(Transaction.MIN_BASE_FEE.toLong())
        .setTimeout(300)
        .build()

    val preparedTransaction = sorobanServer.prepareTransaction(transaction)
    preparedTransaction.sign(anchorKeyPair)

    val response = sorobanServer.sendTransaction(preparedTransaction)
    println("Transaction response: $response")
  }
}
