package org.stellar.anchor.platform.action

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Instant
import javax.validation.ConstraintViolation
import javax.validation.Validator
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.exception.rpc.InvalidParamsException
import org.stellar.anchor.api.exception.rpc.InvalidRequestException
import org.stellar.anchor.api.platform.GetTransactionResponse
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind.DEPOSIT
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24
import org.stellar.anchor.api.rpc.action.NotifyTransactionExpiredRequest
import org.stellar.anchor.api.sep.SepTransactionStatus.*
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.horizon.Horizon
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.sep24.Sep24TransactionStore
import org.stellar.anchor.sep31.Sep31TransactionStore
import org.stellar.anchor.util.GsonUtils

class NotifyTransactionExpiredHandlerTest {

  companion object {
    private val gson = GsonUtils.getInstance()
    private const val TX_ID = "testId"
    private const val TX_MESSAGE = "testMessage"
  }

  @MockK(relaxed = true) private lateinit var txn24Store: Sep24TransactionStore

  @MockK(relaxed = true) private lateinit var txn31Store: Sep31TransactionStore

  @MockK(relaxed = true) private lateinit var horizon: Horizon

  @MockK(relaxed = true) private lateinit var validator: Validator

  @MockK(relaxed = true) private lateinit var assetService: AssetService

  private lateinit var handler: NotifyTransactionExpiredHandler

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    this.handler =
      NotifyTransactionExpiredHandler(txn24Store, txn31Store, validator, horizon, assetService)
  }

  @Test
  fun test_handle_unsupportedProtocol() {
    val request = NotifyTransactionExpiredRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = ERROR.toString()
    val spyTxn24 = spyk(txn24)

    every { txn24Store.findByTransactionId(TX_ID) } returns spyTxn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { spyTxn24.protocol } returns "100"

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals("Protocol[100] is not supported by action[notify_transaction_expired]", ex.message)
  }

  @Test
  fun test_handle_unsupportedStatus_errorStatus() {
    val request = NotifyTransactionExpiredRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = EXPIRED.toString()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Action[notify_transaction_expired] is not supported for status[expired]",
      ex.message
    )
  }

  @Test
  fun test_handle_unsupportedStatus_finalStatus() {
    val request = NotifyTransactionExpiredRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = COMPLETED.toString()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Action[notify_transaction_expired] is not supported for status[completed]",
      ex.message
    )
  }

  @Test
  fun test_handle_missingMessage() {
    val request = NotifyTransactionExpiredRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("message is required", ex.message)
  }

  @Test
  fun test_handle_invalidRequest() {
    val request = NotifyTransactionExpiredRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.kind = DEPOSIT.kind

    val violation1: ConstraintViolation<NotifyTransactionExpiredRequest> = mockk()
    val violation2: ConstraintViolation<NotifyTransactionExpiredRequest> = mockk()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { violation1.message } returns "violation error message 1"
    every { violation2.message } returns "violation error message 2"
    every { validator.validate(request) } returns setOf(violation1, violation2)

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals(
      "violation error message 1\n" + "violation error message 2",
      ex.message?.trimIndent()
    )
  }

  @Test
  fun test_handle_ok() {
    val request =
      NotifyTransactionExpiredRequest.builder().transactionId(TX_ID).message(TX_MESSAGE).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.kind = DEPOSIT.kind
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn31Store.save(any()) }

    val expectedSep24Txn = JdbcSep24Transaction()
    expectedSep24Txn.kind = DEPOSIT.kind
    expectedSep24Txn.status = EXPIRED.toString()
    expectedSep24Txn.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedSep24Txn.message = TX_MESSAGE

    JSONAssert.assertEquals(
      gson.toJson(expectedSep24Txn),
      gson.toJson(sep24TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = SEP_24
    expectedResponse.kind = DEPOSIT
    expectedResponse.status = EXPIRED
    expectedResponse.amountExpected = Amount(null, "")
    expectedResponse.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedResponse.message = TX_MESSAGE

    JSONAssert.assertEquals(
      gson.toJson(expectedResponse),
      gson.toJson(response),
      JSONCompareMode.STRICT
    )

    assertTrue(expectedSep24Txn.updatedAt >= startDate)
    assertTrue(expectedSep24Txn.updatedAt <= endDate)
  }
}
