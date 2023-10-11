package org.stellar.anchor.platform.rpc

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.rpc.InvalidRequestException
import org.stellar.anchor.api.rpc.method.NotifyInteractiveFlowCompletedRequest
import org.stellar.anchor.api.rpc.method.RpcMethod
import org.stellar.anchor.api.rpc.method.RpcMethod.NOTIFY_INTERACTIVE_FLOW_COMPLETED
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.sep.SepTransactionStatus.*
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.event.EventService
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.platform.data.JdbcSepTransaction
import org.stellar.anchor.platform.validator.RequestValidator
import org.stellar.anchor.sep24.Sep24TransactionStore
import org.stellar.anchor.sep31.Sep31TransactionStore

class RpcMethodHandlerTest {

  // test implementation
  class RpcMethodHandlerTestImpl(
    txn24Store: Sep24TransactionStore,
    txn31Store: Sep31TransactionStore,
    requestValidator: RequestValidator,
    assetService: AssetService,
    eventService: EventService
  ) :
    RpcMethodHandler<NotifyInteractiveFlowCompletedRequest>(
      txn24Store,
      txn31Store,
      requestValidator,
      assetService,
      eventService,
      NotifyInteractiveFlowCompletedRequest::class.java
    ) {
    override fun getRpcMethod(): RpcMethod {
      return NOTIFY_INTERACTIVE_FLOW_COMPLETED
    }

    override fun getSupportedStatuses(txn: JdbcSepTransaction?): Set<SepTransactionStatus> {
      return setOf(INCOMPLETE, ERROR)
    }

    override fun updateTransactionWithRpcRequest(
      txn: JdbcSepTransaction?,
      request: NotifyInteractiveFlowCompletedRequest?
    ) {}

    override fun getNextStatus(
      txn: JdbcSepTransaction?,
      request: NotifyInteractiveFlowCompletedRequest?
    ): SepTransactionStatus {
      return PENDING_ANCHOR
    }
  }

  companion object {
    private const val TX_ID = "testId"
  }

  @MockK(relaxed = true) private lateinit var txn24Store: Sep24TransactionStore

  @MockK(relaxed = true) private lateinit var txn31Store: Sep31TransactionStore

  @MockK(relaxed = true) private lateinit var requestValidator: RequestValidator

  @MockK(relaxed = true) private lateinit var assetService: AssetService

  @MockK(relaxed = true) private lateinit var eventService: EventService

  private lateinit var handler: RpcMethodHandler<NotifyInteractiveFlowCompletedRequest>

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    this.handler =
      RpcMethodHandlerTestImpl(txn24Store, txn31Store, requestValidator, assetService, eventService)
  }

  @Test
  fun test_handle_transactionIsNotFound() {
    val request = NotifyInteractiveFlowCompletedRequest.builder().transactionId(TX_ID).build()
    val tnx24 = JdbcSep24Transaction()
    tnx24.status = INCOMPLETE.toString()

    every { txn24Store.findByTransactionId(any()) } returns null
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals("Transaction with id[testId] is not found", ex.message)

    verify(exactly = 0) { txn24Store.save(any()) }
    verify(exactly = 0) { txn31Store.save(any()) }
  }

  @Test
  fun test_isErrorStatus() {
    setOf(ERROR, EXPIRED).forEach { s -> assertTrue(handler.isErrorStatus(s)) }

    Arrays.stream(SepTransactionStatus.values())
      .filter { s -> !setOf(ERROR, EXPIRED).contains(s) }
      .forEach { s -> assertFalse(handler.isErrorStatus(s)) }
  }

  @Test
  fun test_isFinalStatus() {
    setOf(REFUNDED, COMPLETED).forEach { s -> assertTrue(handler.isFinalStatus(s)) }

    Arrays.stream(SepTransactionStatus.values())
      .filter { s -> !setOf(REFUNDED, COMPLETED).contains(s) }
      .forEach { s -> assertFalse(handler.isFinalStatus(s)) }
  }
}
