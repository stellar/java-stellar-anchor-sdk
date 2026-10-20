package org.stellar.reference.client

import kotlin.random.Random
import org.stellar.sdk.AbstractTransaction.MIN_BASE_FEE
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Network
import org.stellar.sdk.SorobanServer
import org.stellar.sdk.TransactionBuilder
import org.stellar.sdk.operations.Operation
import org.stellar.sdk.responses.sorobanrpc.SendTransactionResponse
import org.stellar.sdk.responses.sorobanrpc.SendTransactionResponse.SendTransactionStatus
import org.stellar.sdk.xdr.TransactionResult
import org.stellar.sdk.xdr.TransactionResultCode

private const val MAX_SEND_ATTEMPTS = 5

fun sendWithSequenceRetry(
  rpc: SorobanServer,
  keyPair: KeyPair,
  operation: Operation,
  pause: () -> Unit = { Thread.sleep(Random.nextLong(1000, 4000)) },
): String {
  repeat(MAX_SEND_ATTEMPTS) { attempt ->
    val transaction =
      TransactionBuilder(rpc.getAccount(keyPair.accountId), Network.TESTNET)
        .addOperation(operation)
        .setBaseFee(MIN_BASE_FEE)
        .setTimeout(300)
        .build()
    val prepared = rpc.prepareTransaction(transaction)
    prepared.sign(keyPair)

    val response = rpc.sendTransaction(prepared)
    when {
      response.status == SendTransactionStatus.PENDING ||
        response.status == SendTransactionStatus.DUPLICATE -> return response.hash
      isRetryable(response) && attempt < MAX_SEND_ATTEMPTS - 1 -> pause()
      else ->
        throw RuntimeException(
          "sendTransaction rejected: status=${response.status}, errorResultXdr=${response.errorResultXdr}"
        )
    }
  }
  throw IllegalStateException("unreachable")
}

private fun isRetryable(response: SendTransactionResponse): Boolean =
  when (response.status) {
    SendTransactionStatus.TRY_AGAIN_LATER -> true
    SendTransactionStatus.ERROR ->
      response.errorResultXdr?.let {
        TransactionResult.fromXdrBase64(it).result.discriminant == TransactionResultCode.txBAD_SEQ
      }
        ?: false
    else -> false
  }
