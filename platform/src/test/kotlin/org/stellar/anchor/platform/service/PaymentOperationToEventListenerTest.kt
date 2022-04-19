package org.stellar.anchor.platform.service

import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.event.EventService
import org.stellar.anchor.model.Sep24Transaction
import org.stellar.anchor.platform.paymentobserver.ObservedPayment
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
  fun test_onReceiver() {
    // Payment missing transaction hash shouldn't trigger an event nor reach the DB
    val p = ObservedPayment.builder().build()
    p.transactionHash = null
    p.transactionMemo = "my_memo"
    paymentOperationToEventListener.onReceived(p)
    verify { eventService wasNot Called }
    verify { transactionStore wasNot Called }

    // Payment missing transaction hash shouldn't trigger an event nor reach the DB
    p.transactionHash = "1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30"
    p.transactionMemo = null
    paymentOperationToEventListener.onReceived(p)
    verify { eventService wasNot Called }
    verify { transactionStore wasNot Called }

    // Payment whose memo is not in the DB shouldn't trigger event
    p.transactionHash = "1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30"
    p.transactionMemo = "my_memo"
    val slotMemo = slot<String>()
    every { transactionStore.findByStellarMemo(capture(slotMemo)) } returns null
    paymentOperationToEventListener.onReceived(p)
    verify { eventService wasNot Called }
    verify(exactly = 1) { transactionStore.findByStellarMemo(any()) }
    assertEquals("my_memo", slotMemo.captured)
  }
}