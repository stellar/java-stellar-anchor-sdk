package org.stellar.anchor.platform.action

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.api.exception.rpc.InvalidParamsException
import org.stellar.anchor.api.exception.rpc.InvalidRequestException
import org.stellar.anchor.api.platform.GetTransactionResponse
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind.RECEIVE
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep.*
import org.stellar.anchor.api.rpc.action.RequestCustomerInfoUpdateRequest
import org.stellar.anchor.api.sep.SepTransactionStatus.*
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.Customers
import org.stellar.anchor.api.shared.StellarId
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.event.EventService
import org.stellar.anchor.event.EventService.Session
import org.stellar.anchor.platform.data.JdbcSep31Transaction
import org.stellar.anchor.platform.validator.RequestValidator
import org.stellar.anchor.sep24.Sep24TransactionStore
import org.stellar.anchor.sep31.Sep31TransactionStore
import org.stellar.anchor.util.GsonUtils

class RequestCustomerInfoUpdateHandlerTest {

  companion object {
    private val gson = GsonUtils.getInstance()
    private const val TX_ID = "testId"
    private const val VALIDATION_ERROR_MESSAGE = "Invalid request"
  }

  @MockK(relaxed = true) private lateinit var txn24Store: Sep24TransactionStore

  @MockK(relaxed = true) private lateinit var txn31Store: Sep31TransactionStore

  @MockK(relaxed = true) private lateinit var requestValidator: RequestValidator

  @MockK(relaxed = true) private lateinit var assetService: AssetService

  @MockK(relaxed = true) private lateinit var eventService: EventService

  @MockK(relaxed = true) private lateinit var eventSession: Session

  @MockK(relaxed = true) private lateinit var sepTransactionCounter: Counter

  private lateinit var handler: RequestCustomerInfoUpdateHandler

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { eventService.createSession(any(), EventService.EventQueue.TRANSACTION) } returns
      eventSession
    this.handler =
      RequestCustomerInfoUpdateHandler(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService
      )
  }

  @Test
  fun test_handle_unsupportedProtocol() {
    val request = RequestCustomerInfoUpdateRequest.builder().transactionId(TX_ID).build()
    val txn31 = JdbcSep31Transaction()
    txn31.status = PENDING_RECEIVER.toString()
    val spyTxn31 = spyk(txn31)

    every { txn24Store.findByTransactionId(any()) } returns null
    every { txn31Store.findByTransactionId(TX_ID) } returns spyTxn31
    every { spyTxn31.protocol } returns SEP_38.sep.toString()

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Action[request_customer_info_update] is not supported. Status[pending_receiver], kind[null], protocol[38], funds received[false]",
      ex.message
    )

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_unsupportedStatus() {
    val request = RequestCustomerInfoUpdateRequest.builder().transactionId(TX_ID).build()
    val txn31 = JdbcSep31Transaction()
    txn31.status = INCOMPLETE.toString()

    every { txn24Store.findByTransactionId(any()) } returns null
    every { txn31Store.findByTransactionId(TX_ID) } returns txn31

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Action[request_customer_info_update] is not supported. Status[incomplete], kind[receive], protocol[31], funds received[false]",
      ex.message
    )

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_invalidRequest() {
    val request = RequestCustomerInfoUpdateRequest.builder().transactionId(TX_ID).build()
    val txn31 = JdbcSep31Transaction()
    txn31.status = PENDING_RECEIVER.toString()

    every { txn24Store.findByTransactionId(any()) } returns null
    every { txn31Store.findByTransactionId(TX_ID) } returns txn31
    every { requestValidator.validate(request) } throws
      InvalidParamsException(VALIDATION_ERROR_MESSAGE)

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals(VALIDATION_ERROR_MESSAGE, ex.message?.trimIndent())

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
    verify(exactly = 0) { sepTransactionCounter.increment() }
  }

  @Test
  fun test_handle_ok() {
    val request = RequestCustomerInfoUpdateRequest.builder().transactionId(TX_ID).build()
    val txn31 = JdbcSep31Transaction()
    txn31.status = PENDING_RECEIVER.toString()
    val sep31TxnCapture = slot<JdbcSep31Transaction>()
    val anchorEventCapture = slot<AnchorEvent>()

    mockkStatic(Metrics::class)

    every { txn24Store.findByTransactionId(any()) } returns null
    every { txn31Store.findByTransactionId(TX_ID) } returns txn31
    every { txn31Store.save(capture(sep31TxnCapture)) } returns null
    every { eventSession.publish(capture(anchorEventCapture)) } just Runs
    every { Metrics.counter("sep31.transaction", "status", "pending_customer_info_update") } returns
      sepTransactionCounter

    val startDate = Instant.now()
    val response = handler.handle(request)
    val endDate = Instant.now()

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 1) { sepTransactionCounter.increment() }

    val expectedSep31Txn = JdbcSep31Transaction()
    expectedSep31Txn.status = PENDING_CUSTOMER_INFO_UPDATE.toString()
    expectedSep31Txn.updatedAt = sep31TxnCapture.captured.updatedAt

    JSONAssert.assertEquals(
      gson.toJson(expectedSep31Txn),
      gson.toJson(sep31TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = SEP_31
    expectedResponse.kind = RECEIVE
    expectedResponse.status = PENDING_CUSTOMER_INFO_UPDATE
    expectedResponse.amountIn = Amount()
    expectedResponse.amountOut = Amount()
    expectedResponse.amountFee = Amount()
    expectedResponse.amountExpected = Amount()
    expectedResponse.updatedAt = sep31TxnCapture.captured.updatedAt
    expectedResponse.customers = Customers(StellarId(), StellarId())

    JSONAssert.assertEquals(
      gson.toJson(expectedResponse),
      gson.toJson(response),
      JSONCompareMode.STRICT
    )

    val expectedEvent =
      AnchorEvent.builder()
        .id(anchorEventCapture.captured.id)
        .sep(SEP_31.sep.toString())
        .type(AnchorEvent.Type.TRANSACTION_STATUS_CHANGED)
        .transaction(expectedResponse)
        .build()

    JSONAssert.assertEquals(
      gson.toJson(expectedEvent),
      gson.toJson(anchorEventCapture.captured),
      JSONCompareMode.STRICT
    )

    assertTrue(sep31TxnCapture.captured.updatedAt >= startDate)
    assertTrue(sep31TxnCapture.captured.updatedAt <= endDate)
  }
}
