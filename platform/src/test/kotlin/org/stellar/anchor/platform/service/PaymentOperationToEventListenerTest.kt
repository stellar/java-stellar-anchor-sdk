package org.stellar.anchor.platform.service

import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.event.EventService
import org.stellar.anchor.model.Sep31TransactionBuilder
import org.stellar.anchor.platform.paymentobserver.ObservedPayment
import org.stellar.anchor.sep31.Sep31TransactionStore
import org.stellar.anchor.server.data.JdbcSep31Transaction
import org.stellar.anchor.server.data.JdbcSep31TransactionStore
import kotlin.test.assertEquals

class PaymentOperationToEventListenerTest {
  @MockK(relaxed = true) private lateinit var transactionStore: JdbcSep31TransactionStore
  @MockK(relaxed = true) private lateinit var eventService: EventService
  private lateinit var paymentOperationToEventListener: PaymentOperationToEventListener

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    paymentOperationToEventListener = PaymentOperationToEventListener(transactionStore, eventService)
  }

  @Test
  fun test_onReceiver_validation() {
    // Payment missing txHash shouldn't trigger an event nor reach the DB
    val p = ObservedPayment.builder().build()
    p.transactionHash = null
    p.transactionMemo = "my_memo_1"
    paymentOperationToEventListener.onReceived(p)
    verify { eventService wasNot Called }
    verify { transactionStore wasNot Called }

    // Payment missing txHash shouldn't trigger an event nor reach the DB
    p.transactionHash = "1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30"
    p.transactionMemo = null
    paymentOperationToEventListener.onReceived(p)
    verify { eventService wasNot Called }
    verify { transactionStore wasNot Called }

    // Asset types different from "credit_alphanum4" and "credit_alphanum12" shouldn't trigger an event nor reach the DB
    p.transactionHash = "1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30"
    p.transactionMemo = "my_memo_1"
    p.assetType = "native"
    paymentOperationToEventListener.onReceived(p)
    verify { eventService wasNot Called }
    verify { transactionStore wasNot Called }

    // Payment whose memo is not in the DB shouldn't trigger event
    p.transactionHash = "1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30"
    p.transactionMemo = "my_memo_2"
    p.assetType = "credit_alphanum4"
    var slotMemo = slot<String>()
    every { transactionStore.findByStellarMemo(capture(slotMemo)) } returns null
    paymentOperationToEventListener.onReceived(p)
    verify { eventService wasNot Called }
    verify(exactly = 1) { transactionStore.findByStellarMemo("my_memo_2") }
    assertEquals("my_memo_2", slotMemo.captured)

    // If findByStellarMemo throws an exception, we shouldn't trigger an event
    slotMemo = slot()
    p.transactionMemo = "my_memo_3"
    every { transactionStore.findByStellarMemo(capture(slotMemo)) } throws RuntimeException("Something went wrong")
    paymentOperationToEventListener.onReceived(p)
    verify { eventService wasNot Called }
    verify(exactly = 1) { transactionStore.findByStellarMemo("my_memo_3") }
    assertEquals("my_memo_3", slotMemo.captured)

    // If asset code from the fetched tx is different, don't trigger event
    slotMemo = slot()
    p.transactionMemo = "my_memo_4"
    p.assetCode = "FOO"
    val sep31TxMock = JdbcSep31Transaction()
    sep31TxMock.amountInAsset = "BAR"
    every { transactionStore.findByStellarMemo(capture(slotMemo)) } returns sep31TxMock
    paymentOperationToEventListener.onReceived(p)
    verify { eventService wasNot Called }
    verify(exactly = 1) { transactionStore.findByStellarMemo("my_memo_4") }
    assertEquals("my_memo_4", slotMemo.captured)
  }
}