package org.stellar.reference.client

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.sdk.Account
import org.stellar.sdk.AssetTypeNative
import org.stellar.sdk.KeyPair
import org.stellar.sdk.SorobanServer
import org.stellar.sdk.operations.PaymentOperation
import org.stellar.sdk.responses.sorobanrpc.SendTransactionResponse
import org.stellar.sdk.responses.sorobanrpc.SendTransactionResponse.SendTransactionStatus
import org.stellar.sdk.xdr.Int64
import org.stellar.sdk.xdr.TransactionResult
import org.stellar.sdk.xdr.TransactionResultCode

class SequenceRetryTest {
  private val keyPair = KeyPair.random()
  private val operation =
    PaymentOperation.builder()
      .destination(KeyPair.random().accountId)
      .asset(AssetTypeNative())
      .amount(java.math.BigDecimal.ONE)
      .build()
  private lateinit var rpc: SorobanServer

  @BeforeEach
  fun setUp() {
    rpc = mockk()
    every { rpc.getAccount(keyPair.accountId) } answers { Account(keyPair.accountId, 1L) }
    every { rpc.prepareTransaction(any()) } answers { firstArg() }
  }

  @Test
  fun `returns the hash when the first send is accepted`() {
    every { rpc.sendTransaction(any()) } returns response(SendTransactionStatus.PENDING)

    assertEquals("hash", sendWithSequenceRetry(rpc, keyPair, operation) {})
    verify(exactly = 1) { rpc.sendTransaction(any()) }
  }

  @Test
  fun `reloads the sequence and retries after txBAD_SEQ`() {
    every { rpc.sendTransaction(any()) } returnsMany
      listOf(error(TransactionResultCode.txBAD_SEQ), response(SendTransactionStatus.PENDING))

    assertEquals("hash", sendWithSequenceRetry(rpc, keyPair, operation) {})
    verify(exactly = 2) { rpc.getAccount(keyPair.accountId) }
    verify(exactly = 2) { rpc.sendTransaction(any()) }
  }

  @Test
  fun `retries after TRY_AGAIN_LATER`() {
    every { rpc.sendTransaction(any()) } returnsMany
      listOf(
        response(SendTransactionStatus.TRY_AGAIN_LATER),
        response(SendTransactionStatus.DUPLICATE),
      )

    assertEquals("hash", sendWithSequenceRetry(rpc, keyPair, operation) {})
    verify(exactly = 2) { rpc.sendTransaction(any()) }
  }

  @Test
  fun `does not retry other errors`() {
    every { rpc.sendTransaction(any()) } returns error(TransactionResultCode.txINSUFFICIENT_BALANCE)

    assertThrows<RuntimeException> { sendWithSequenceRetry(rpc, keyPair, operation) {} }
    verify(exactly = 1) { rpc.sendTransaction(any()) }
  }

  @Test
  fun `gives up after 5 attempts`() {
    every { rpc.sendTransaction(any()) } returns error(TransactionResultCode.txBAD_SEQ)

    assertThrows<RuntimeException> { sendWithSequenceRetry(rpc, keyPair, operation) {} }
    verify(exactly = 5) { rpc.sendTransaction(any()) }
  }

  private fun response(status: SendTransactionStatus): SendTransactionResponse {
    val response = mockk<SendTransactionResponse>()
    every { response.status } returns status
    every { response.hash } returns "hash"
    every { response.errorResultXdr } returns null
    return response
  }

  private fun error(code: TransactionResultCode): SendTransactionResponse {
    val result =
      TransactionResult.builder()
        .feeCharged(Int64(100L))
        .result(TransactionResult.TransactionResultResult.builder().discriminant(code).build())
        .ext(TransactionResult.TransactionResultExt.builder().discriminant(0).build())
        .build()
    val response = mockk<SendTransactionResponse>()
    every { response.status } returns SendTransactionStatus.ERROR
    every { response.hash } returns "hash"
    every { response.errorResultXdr } returns result.toXdrBase64()
    return response
  }
}
