package org.stellar.anchor.platform.service

import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.event.EventService
import org.stellar.anchor.platform.paymentobserver.ObservedPayment
import org.stellar.anchor.server.data.JdbcSep31TransactionStore

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
    p.transactionMemo = "my_memo"
    paymentOperationToEventListener.onReceived(p)
    verify { eventService wasNot Called }
    verify { transactionStore wasNot Called }

    // Payment missing transaction hash shouldn't trigger an event nor reach the DB
    p.transactionMemo = null
    p.transactionHash = "1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30"
    paymentOperationToEventListener.onReceived(p)
    verify { eventService wasNot Called }
    verify { transactionStore wasNot Called }
  }
}