package org.stellar.anchor.platform.service

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.Customers
import org.stellar.anchor.api.shared.StellarId
import org.stellar.anchor.api.shared.StellarPayment
import org.stellar.anchor.api.shared.StellarTransaction
import org.stellar.anchor.event.EventPublishService
import org.stellar.anchor.event.models.*
import org.stellar.anchor.platform.data.JdbcSep31Transaction
import org.stellar.anchor.platform.data.JdbcSep31TransactionStore
import org.stellar.anchor.platform.payment.observer.circle.ObservedPayment
import org.stellar.anchor.sep31.Sep31Transaction
import org.stellar.anchor.util.GsonUtils

class PaymentOperationToEventListenerTest {
  @MockK(relaxed = true) private lateinit var transactionStore: JdbcSep31TransactionStore
  @MockK(relaxed = true) private lateinit var eventPublishService: EventPublishService
  private lateinit var paymentOperationToEventListener: PaymentOperationToEventListener
  private val gson = GsonUtils.getInstance()

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    paymentOperationToEventListener =
      PaymentOperationToEventListener(transactionStore, eventPublishService)
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun test_onReceiver_failValidation() {
    // Payment missing txHash shouldn't trigger an event nor reach the DB
    val p = ObservedPayment.builder().build()
    p.transactionHash = null
    p.transactionMemoType = "text"
    p.transactionMemo = "my_memo_1"
    paymentOperationToEventListener.onReceived(p)
    verify { eventPublishService wasNot Called }
    verify { transactionStore wasNot Called }

    // Payment missing txHash shouldn't trigger an event nor reach the DB
    p.transactionHash = "1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30"
    p.transactionMemo = null
    paymentOperationToEventListener.onReceived(p)
    verify { eventPublishService wasNot Called }
    verify { transactionStore wasNot Called }

    // Asset types different from "credit_alphanum4" and "credit_alphanum12" shouldn't trigger an
    // event nor reach the DB
    p.transactionHash = "1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30"
    p.transactionMemo = "my_memo_1"
    p.assetType = "native"
    paymentOperationToEventListener.onReceived(p)
    verify { eventPublishService wasNot Called }
    verify { transactionStore wasNot Called }

    // Payment whose memo is not in the DB shouldn't trigger event
    p.transactionHash = "1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30"
    p.transactionMemo = "my_memo_2"
    p.assetType = "credit_alphanum4"
    var slotMemo = slot<String>()
    every { transactionStore.findByStellarMemo(capture(slotMemo)) } returns null
    paymentOperationToEventListener.onReceived(p)
    verify { eventPublishService wasNot Called }
    verify(exactly = 1) { transactionStore.findByStellarMemo("my_memo_2") }
    assertEquals("my_memo_2", slotMemo.captured)

    // If findByStellarMemo throws an exception, we shouldn't trigger an event
    slotMemo = slot()
    p.transactionMemo = "my_memo_3"
    every { transactionStore.findByStellarMemo(capture(slotMemo)) } throws
      SepException("Something went wrong")
    paymentOperationToEventListener.onReceived(p)
    verify { eventPublishService wasNot Called }
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
    verify { eventPublishService wasNot Called }
    verify(exactly = 1) { transactionStore.findByStellarMemo("my_memo_4") }
    assertEquals("my_memo_4", slotMemo.captured)
  }

  @Test
  fun `test onReceiver gets the expected amount and sends the event with PENDING_RECEIVER status`() {
    val startedAtMock = Instant.now().minusSeconds(120)
    val transferReceivedAt = Instant.now()
    val transferReceivedAtStr = DateTimeFormatter.ISO_INSTANT.format(transferReceivedAt)
    val fooAsset = "stellar:FOO:GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364"
    val barAsset = "stellar:BAR:GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364"

    val p = ObservedPayment.builder().build()
    p.transactionHash = "1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30"
    p.transactionMemo = "39623738663066612d393366392d343139382d386439332d6537366664303834"
    p.transactionMemoType = "hash"
    p.assetType = "credit_alphanum4"
    p.assetCode = "FOO"
    p.assetIssuer = "GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364"
    p.assetName = "FOO:GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364"
    p.amount = "10.0000000"
    p.sourceAccount = "GCJKWN7ELKOXLDHJTOU4TZOEJQL7TYVVTQFR676MPHHUIUDAHUA7QGJ4"
    p.from = "GAJKV32ZXP5QLYHPCMLTV5QCMNJR3W6ZKFP6HMDN67EM2ULDHHDGEZYO"
    p.to = "GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364"
    p.type = ObservedPayment.Type.PATH_PAYMENT
    p.createdAt = transferReceivedAtStr
    p.transactionEnvelope =
      "AAAAAgAAAAAQfdFrLDgzSIIugR73qs8U0ZiKbwBUclTTPh5thlbgnAAAB9AAAACwAAAABAAAAAEAAAAAAAAAAAAAAABiMbeEAAAAAAAAABQAAAAAAAAAAAAAAADcXPrnCDi+IDcGSvu/HjP779qjBv6K9Sie8i3WDySaIgAAAAA8M2CAAAAAAAAAAAAAAAAAJXdMB+xylKwEPk1tOLU82vnDM0u15RsK6/HCKsY1O3MAAAAAPDNggAAAAAAAAAAAAAAAALn+JaJ9iXEcrPeRFqEMGo6WWFeOwW15H/vvCOuMqCsSAAAAADwzYIAAAAAAAAAAAAAAAADbWpHlX0LQjIjY0x8jWkclnQDK8jFmqhzCmB+1EusXwAAAAAA8M2CAAAAAAAAAAAAAAAAAmy3UTqTnhNzIg8TjCYiRh9l07ls0Hi5FTqelhfZ4KqAAAAAAPDNggAAAAAAAAAAAAAAAAIwiZIIbYJn7MbHrrM+Pg85c6Lcn0ZGLb8NIiXLEIPTnAAAAADwzYIAAAAAAAAAAAAAAAAAYEjPKA/6lDpr/w1Cfif2hK4GHeNODhw0kk4kgLrmPrQAAAAA8M2CAAAAAAAAAAAAAAAAASMrE32C3vL39cj84pIg2mt6OkeWBz5OSZn0eypcjS4IAAAAAPDNggAAAAAAAAAAAAAAAAIuxsI+2mSeh3RkrkcpQ8bMqE7nXUmdvgwyJS/dBThIPAAAAADwzYIAAAAAAAAAAAAAAAACuZxdjR/GXaymdc9y5WFzz2A8Yk5hhgzBZsQ9R0/BmZwAAAAA8M2CAAAAAAAAAAAAAAAAAAtWBvyq0ToNovhQHSLeQYu7UzuqbVrm0i3d1TjRm7WEAAAAAPDNggAAAAAAAAAAAAAAAANtrzNON0u1IEGKmVsm80/Av+BKip0ioeS/4E+Ejs9YPAAAAADwzYIAAAAAAAAAAAAAAAAD+ejNcgNcKjR/ihUx1ikhdz5zmhzvRET3LGd7oOiBlTwAAAAA8M2CAAAAAAAAAAAAAAAAASXG3P6KJjS6e0dzirbso8vRvZKo6zETUsEv7OSP8XekAAAAAPDNggAAAAAAAAAAAAAAAAC5orVpxxvGEB8ISTho2YdOPZJrd7UBj1Bt8TOjLOiEKAAAAADwzYIAAAAAAAAAAAAAAAAAOQR7AqdGyIIMuFLw9JQWtHqsUJD94kHum7SJS9PXkOwAAAAA8M2CAAAAAAAAAAAAAAAAAIosijRx7xSP/+GA6eAjGeV9wJtKDySP+OJr90euE1yQAAAAAPDNggAAAAAAAAAAAAAAAAKlHXWQvwNPeT4Pp1oJDiOpcKwS3d9sho+ha+6pyFwFqAAAAADwzYIAAAAAAAAAAAAAAAABjCjnoL8+FEP0LByZA9PfMLwU1uAX4Cb13rVs83e1UZAAAAAA8M2CAAAAAAAAAAAAAAAAAokhNCZNGq9uAkfKTNoNGr5XmmMoY5poQEmp8OVbit7IAAAAAPDNggAAAAAAAAAABhlbgnAAAAEBa9csgF5/0wxrYM6oVsbM4Yd+/3uVIplS6iLmPOS4xf8oLQLtjKKKIIKmg9Gc/yYm3icZyU7icy9hGjcujenMN"
    p.id = "755914248193"

    val senderId = "d2bd1412-e2f6-4047-ad70-a1a2f133b25c"
    val receiverId = "137938d4-43a7-4252-a452-842adcee474c"

    val slotMemo = slot<String>()
    val sep31TxMock = JdbcSep31Transaction()
    sep31TxMock.id = "ceaa7677-a5a7-434e-b02a-8e0801b3e7bd"
    sep31TxMock.amountExpected = "10"
    sep31TxMock.amountIn = "10"
    sep31TxMock.amountInAsset = fooAsset
    sep31TxMock.amountOut = "20"
    sep31TxMock.amountOutAsset = barAsset
    sep31TxMock.amountFee = "0.5"
    sep31TxMock.amountFeeAsset = fooAsset
    sep31TxMock.quoteId = "cef1fc13-3f65-4612-b1f2-502d698c816b"
    sep31TxMock.startedAt = startedAtMock
    sep31TxMock.updatedAt = startedAtMock
    sep31TxMock.transferReceivedAt = null // the event should have a valid `transferReceivedAt`
    sep31TxMock.stellarMemo = "OWI3OGYwZmEtOTNmOS00MTk4LThkOTMtZTc2ZmQwODQ="
    sep31TxMock.stellarMemoType = "hash"
    sep31TxMock.status = SepTransactionStatus.PENDING_SENDER.toString()
    sep31TxMock.senderId = senderId
    sep31TxMock.receiverId = receiverId
    sep31TxMock.creator =
      StellarId.builder()
        .account("GBE4B7KE62NUBFLYT3BIG4OP5DAXBQX2GSZZOVAYXQKJKIU7P6V2R2N4")
        .build()

    val sep31TxCopy = gson.fromJson(gson.toJson(sep31TxMock), JdbcSep31Transaction::class.java)
    every { transactionStore.findByStellarMemo(capture(slotMemo)) } returns sep31TxCopy

    val slotTx = slot<Sep31Transaction>()
    every { transactionStore.save(capture(slotTx)) } returns sep31TxMock

    val stellarTransaction =
      StellarTransaction.builder()
        .id("1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30")
        .memo("OWI3OGYwZmEtOTNmOS00MTk4LThkOTMtZTc2ZmQwODQ=")
        .memoType("hash")
        .createdAt(transferReceivedAt)
        .envelope(
          "AAAAAgAAAAAQfdFrLDgzSIIugR73qs8U0ZiKbwBUclTTPh5thlbgnAAAB9AAAACwAAAABAAAAAEAAAAAAAAAAAAAAABiMbeEAAAAAAAAABQAAAAAAAAAAAAAAADcXPrnCDi+IDcGSvu/HjP779qjBv6K9Sie8i3WDySaIgAAAAA8M2CAAAAAAAAAAAAAAAAAJXdMB+xylKwEPk1tOLU82vnDM0u15RsK6/HCKsY1O3MAAAAAPDNggAAAAAAAAAAAAAAAALn+JaJ9iXEcrPeRFqEMGo6WWFeOwW15H/vvCOuMqCsSAAAAADwzYIAAAAAAAAAAAAAAAADbWpHlX0LQjIjY0x8jWkclnQDK8jFmqhzCmB+1EusXwAAAAAA8M2CAAAAAAAAAAAAAAAAAmy3UTqTnhNzIg8TjCYiRh9l07ls0Hi5FTqelhfZ4KqAAAAAAPDNggAAAAAAAAAAAAAAAAIwiZIIbYJn7MbHrrM+Pg85c6Lcn0ZGLb8NIiXLEIPTnAAAAADwzYIAAAAAAAAAAAAAAAAAYEjPKA/6lDpr/w1Cfif2hK4GHeNODhw0kk4kgLrmPrQAAAAA8M2CAAAAAAAAAAAAAAAAASMrE32C3vL39cj84pIg2mt6OkeWBz5OSZn0eypcjS4IAAAAAPDNggAAAAAAAAAAAAAAAAIuxsI+2mSeh3RkrkcpQ8bMqE7nXUmdvgwyJS/dBThIPAAAAADwzYIAAAAAAAAAAAAAAAACuZxdjR/GXaymdc9y5WFzz2A8Yk5hhgzBZsQ9R0/BmZwAAAAA8M2CAAAAAAAAAAAAAAAAAAtWBvyq0ToNovhQHSLeQYu7UzuqbVrm0i3d1TjRm7WEAAAAAPDNggAAAAAAAAAAAAAAAANtrzNON0u1IEGKmVsm80/Av+BKip0ioeS/4E+Ejs9YPAAAAADwzYIAAAAAAAAAAAAAAAAD+ejNcgNcKjR/ihUx1ikhdz5zmhzvRET3LGd7oOiBlTwAAAAA8M2CAAAAAAAAAAAAAAAAASXG3P6KJjS6e0dzirbso8vRvZKo6zETUsEv7OSP8XekAAAAAPDNggAAAAAAAAAAAAAAAAC5orVpxxvGEB8ISTho2YdOPZJrd7UBj1Bt8TOjLOiEKAAAAADwzYIAAAAAAAAAAAAAAAAAOQR7AqdGyIIMuFLw9JQWtHqsUJD94kHum7SJS9PXkOwAAAAA8M2CAAAAAAAAAAAAAAAAAIosijRx7xSP/+GA6eAjGeV9wJtKDySP+OJr90euE1yQAAAAAPDNggAAAAAAAAAAAAAAAAKlHXWQvwNPeT4Pp1oJDiOpcKwS3d9sho+ha+6pyFwFqAAAAADwzYIAAAAAAAAAAAAAAAABjCjnoL8+FEP0LByZA9PfMLwU1uAX4Cb13rVs83e1UZAAAAAA8M2CAAAAAAAAAAAAAAAAAokhNCZNGq9uAkfKTNoNGr5XmmMoY5poQEmp8OVbit7IAAAAAPDNggAAAAAAAAAABhlbgnAAAAEBa9csgF5/0wxrYM6oVsbM4Yd+/3uVIplS6iLmPOS4xf8oLQLtjKKKIIKmg9Gc/yYm3icZyU7icy9hGjcujenMN"
        )
        .payments(
          listOf(
            StellarPayment.builder()
              .id("755914248193")
              .paymentType(StellarPayment.Type.PATH_PAYMENT)
              .sourceAccount("GAJKV32ZXP5QLYHPCMLTV5QCMNJR3W6ZKFP6HMDN67EM2ULDHHDGEZYO")
              .destinationAccount("GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364")
              .amount(
                Amount("10.0000000", "FOO:GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364")
              )
              .build()
          )
        )
        .build()

    val wantEvent =
      TransactionEvent.builder()
        .type(TransactionEvent.Type.TRANSACTION_STATUS_CHANGED)
        .id("ceaa7677-a5a7-434e-b02a-8e0801b3e7bd")
        .status(TransactionEvent.Status.PENDING_RECEIVER)
        .statusChange(
          TransactionEvent.StatusChange(
            TransactionEvent.Status.PENDING_SENDER,
            TransactionEvent.Status.PENDING_RECEIVER
          )
        )
        .sep(TransactionEvent.Sep.SEP_31)
        .kind(TransactionEvent.Kind.RECEIVE)
        .amountExpected(Amount("10", fooAsset))
        .amountIn(Amount("10.0000000", fooAsset))
        .amountOut(Amount("20", barAsset))
        .amountFee(Amount("0.5", fooAsset))
        .quoteId("cef1fc13-3f65-4612-b1f2-502d698c816b")
        .startedAt(startedAtMock)
        .updatedAt(transferReceivedAt)
        .transferReceivedAt(transferReceivedAt)
        .message("Incoming payment for SEP-31 transaction")
        .sourceAccount("GAJKV32ZXP5QLYHPCMLTV5QCMNJR3W6ZKFP6HMDN67EM2ULDHHDGEZYO")
        .destinationAccount("GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364")
        .creator(
          StellarId.builder()
            .account("GBE4B7KE62NUBFLYT3BIG4OP5DAXBQX2GSZZOVAYXQKJKIU7P6V2R2N4")
            .build()
        )
        .customers(
          Customers(
            StellarId.builder().id(senderId).build(),
            StellarId.builder().id(receiverId).build()
          )
        )
        .stellarTransactions(listOf(stellarTransaction))
        .build()

    val slotEvent = slot<TransactionEvent>()
    every { eventPublishService.publish(capture(slotEvent)) } just Runs

    paymentOperationToEventListener.onReceived(p)
    verify(exactly = 1) {
      transactionStore.findByStellarMemo("OWI3OGYwZmEtOTNmOS00MTk4LThkOTMtZTc2ZmQwODQ=")
    }
    verify(exactly = 1) { eventPublishService.publish(any()) }

    wantEvent.eventId = slotEvent.captured.eventId
    assertEquals(wantEvent, slotEvent.captured)

    // wantSep31Tx
    val wantSep31Tx = gson.fromJson(gson.toJson(sep31TxMock), JdbcSep31Transaction::class.java)
    wantSep31Tx.status = TransactionEvent.Status.PENDING_RECEIVER.status
    wantSep31Tx.stellarTransactionId =
      "1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30"
    wantSep31Tx.transferReceivedAt = transferReceivedAt
    wantSep31Tx.updatedAt = transferReceivedAt
    wantSep31Tx.stellarTransactions = listOf(stellarTransaction)

    assertEquals(wantSep31Tx, slotTx.captured)
  }

  @Test
  fun `test onReceiver gets less than the expected amount it sends the PENDING_RECEIVER status with a message`() {
    val startedAtMock = Instant.now().minusSeconds(120)
    val transferReceivedAt = Instant.now()
    val transferReceivedAtStr = DateTimeFormatter.ISO_INSTANT.format(transferReceivedAt)
    val fooAsset = "stellar:FOO:GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364"
    val barAsset = "stellar:BAR:GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364"

    val p = ObservedPayment.builder().build()
    p.transactionHash = "1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30"
    p.transactionMemo = "39623738663066612d393366392d343139382d386439332d6537366664303834"
    p.transactionMemoType = "hash"
    p.assetType = "credit_alphanum4"
    p.assetCode = "FOO"
    p.assetIssuer = "GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364"
    p.assetName = "FOO:GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364"
    p.amount = "9.0000000"
    p.sourceAccount = "GCJKWN7ELKOXLDHJTOU4TZOEJQL7TYVVTQFR676MPHHUIUDAHUA7QGJ4"
    p.from = "GAJKV32ZXP5QLYHPCMLTV5QCMNJR3W6ZKFP6HMDN67EM2ULDHHDGEZYO"
    p.to = "GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364"
    p.type = ObservedPayment.Type.PAYMENT
    p.createdAt = transferReceivedAtStr
    p.transactionEnvelope =
      "AAAAAgAAAAAQfdFrLDgzSIIugR73qs8U0ZiKbwBUclTTPh5thlbgnAAAB9AAAACwAAAABAAAAAEAAAAAAAAAAAAAAABiMbeEAAAAAAAAABQAAAAAAAAAAAAAAADcXPrnCDi+IDcGSvu/HjP779qjBv6K9Sie8i3WDySaIgAAAAA8M2CAAAAAAAAAAAAAAAAAJXdMB+xylKwEPk1tOLU82vnDM0u15RsK6/HCKsY1O3MAAAAAPDNggAAAAAAAAAAAAAAAALn+JaJ9iXEcrPeRFqEMGo6WWFeOwW15H/vvCOuMqCsSAAAAADwzYIAAAAAAAAAAAAAAAADbWpHlX0LQjIjY0x8jWkclnQDK8jFmqhzCmB+1EusXwAAAAAA8M2CAAAAAAAAAAAAAAAAAmy3UTqTnhNzIg8TjCYiRh9l07ls0Hi5FTqelhfZ4KqAAAAAAPDNggAAAAAAAAAAAAAAAAIwiZIIbYJn7MbHrrM+Pg85c6Lcn0ZGLb8NIiXLEIPTnAAAAADwzYIAAAAAAAAAAAAAAAAAYEjPKA/6lDpr/w1Cfif2hK4GHeNODhw0kk4kgLrmPrQAAAAA8M2CAAAAAAAAAAAAAAAAASMrE32C3vL39cj84pIg2mt6OkeWBz5OSZn0eypcjS4IAAAAAPDNggAAAAAAAAAAAAAAAAIuxsI+2mSeh3RkrkcpQ8bMqE7nXUmdvgwyJS/dBThIPAAAAADwzYIAAAAAAAAAAAAAAAACuZxdjR/GXaymdc9y5WFzz2A8Yk5hhgzBZsQ9R0/BmZwAAAAA8M2CAAAAAAAAAAAAAAAAAAtWBvyq0ToNovhQHSLeQYu7UzuqbVrm0i3d1TjRm7WEAAAAAPDNggAAAAAAAAAAAAAAAANtrzNON0u1IEGKmVsm80/Av+BKip0ioeS/4E+Ejs9YPAAAAADwzYIAAAAAAAAAAAAAAAAD+ejNcgNcKjR/ihUx1ikhdz5zmhzvRET3LGd7oOiBlTwAAAAA8M2CAAAAAAAAAAAAAAAAASXG3P6KJjS6e0dzirbso8vRvZKo6zETUsEv7OSP8XekAAAAAPDNggAAAAAAAAAAAAAAAAC5orVpxxvGEB8ISTho2YdOPZJrd7UBj1Bt8TOjLOiEKAAAAADwzYIAAAAAAAAAAAAAAAAAOQR7AqdGyIIMuFLw9JQWtHqsUJD94kHum7SJS9PXkOwAAAAA8M2CAAAAAAAAAAAAAAAAAIosijRx7xSP/+GA6eAjGeV9wJtKDySP+OJr90euE1yQAAAAAPDNggAAAAAAAAAAAAAAAAKlHXWQvwNPeT4Pp1oJDiOpcKwS3d9sho+ha+6pyFwFqAAAAADwzYIAAAAAAAAAAAAAAAABjCjnoL8+FEP0LByZA9PfMLwU1uAX4Cb13rVs83e1UZAAAAAA8M2CAAAAAAAAAAAAAAAAAokhNCZNGq9uAkfKTNoNGr5XmmMoY5poQEmp8OVbit7IAAAAAPDNggAAAAAAAAAABhlbgnAAAAEBa9csgF5/0wxrYM6oVsbM4Yd+/3uVIplS6iLmPOS4xf8oLQLtjKKKIIKmg9Gc/yYm3icZyU7icy9hGjcujenMN"
    p.id = "755914248193"

    val senderId = "d2bd1412-e2f6-4047-ad70-a1a2f133b25c"
    val receiverId = "137938d4-43a7-4252-a452-842adcee474c"

    val slotMemo = slot<String>()
    val sep31TxMock = JdbcSep31Transaction()
    sep31TxMock.id = "ceaa7677-a5a7-434e-b02a-8e0801b3e7bd"
    sep31TxMock.amountExpected = "10"
    sep31TxMock.amountIn = "10"
    sep31TxMock.amountInAsset = fooAsset
    sep31TxMock.amountOut = "20"
    sep31TxMock.amountOutAsset = barAsset
    sep31TxMock.amountFee = "0.5"
    sep31TxMock.amountFeeAsset = fooAsset
    sep31TxMock.quoteId = "cef1fc13-3f65-4612-b1f2-502d698c816b"
    sep31TxMock.startedAt = startedAtMock
    sep31TxMock.updatedAt = startedAtMock
    sep31TxMock.transferReceivedAt = null // the event should have a valid `transferReceivedAt`
    sep31TxMock.stellarMemo = "OWI3OGYwZmEtOTNmOS00MTk4LThkOTMtZTc2ZmQwODQ="
    sep31TxMock.stellarMemoType = "hash"
    sep31TxMock.status = SepTransactionStatus.PENDING_SENDER.toString()
    sep31TxMock.senderId = senderId
    sep31TxMock.receiverId = receiverId
    sep31TxMock.creator =
      StellarId.builder()
        .account("GBE4B7KE62NUBFLYT3BIG4OP5DAXBQX2GSZZOVAYXQKJKIU7P6V2R2N4")
        .build()

    val sep31TxCopy = gson.fromJson(gson.toJson(sep31TxMock), JdbcSep31Transaction::class.java)
    every { transactionStore.findByStellarMemo(capture(slotMemo)) } returns sep31TxCopy

    val slotTx = slot<Sep31Transaction>()
    every { transactionStore.save(capture(slotTx)) } returns sep31TxMock

    val stellarTransaction =
      StellarTransaction.builder()
        .id("1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30")
        .memo("OWI3OGYwZmEtOTNmOS00MTk4LThkOTMtZTc2ZmQwODQ=")
        .memoType("hash")
        .createdAt(transferReceivedAt)
        .envelope(
          "AAAAAgAAAAAQfdFrLDgzSIIugR73qs8U0ZiKbwBUclTTPh5thlbgnAAAB9AAAACwAAAABAAAAAEAAAAAAAAAAAAAAABiMbeEAAAAAAAAABQAAAAAAAAAAAAAAADcXPrnCDi+IDcGSvu/HjP779qjBv6K9Sie8i3WDySaIgAAAAA8M2CAAAAAAAAAAAAAAAAAJXdMB+xylKwEPk1tOLU82vnDM0u15RsK6/HCKsY1O3MAAAAAPDNggAAAAAAAAAAAAAAAALn+JaJ9iXEcrPeRFqEMGo6WWFeOwW15H/vvCOuMqCsSAAAAADwzYIAAAAAAAAAAAAAAAADbWpHlX0LQjIjY0x8jWkclnQDK8jFmqhzCmB+1EusXwAAAAAA8M2CAAAAAAAAAAAAAAAAAmy3UTqTnhNzIg8TjCYiRh9l07ls0Hi5FTqelhfZ4KqAAAAAAPDNggAAAAAAAAAAAAAAAAIwiZIIbYJn7MbHrrM+Pg85c6Lcn0ZGLb8NIiXLEIPTnAAAAADwzYIAAAAAAAAAAAAAAAAAYEjPKA/6lDpr/w1Cfif2hK4GHeNODhw0kk4kgLrmPrQAAAAA8M2CAAAAAAAAAAAAAAAAASMrE32C3vL39cj84pIg2mt6OkeWBz5OSZn0eypcjS4IAAAAAPDNggAAAAAAAAAAAAAAAAIuxsI+2mSeh3RkrkcpQ8bMqE7nXUmdvgwyJS/dBThIPAAAAADwzYIAAAAAAAAAAAAAAAACuZxdjR/GXaymdc9y5WFzz2A8Yk5hhgzBZsQ9R0/BmZwAAAAA8M2CAAAAAAAAAAAAAAAAAAtWBvyq0ToNovhQHSLeQYu7UzuqbVrm0i3d1TjRm7WEAAAAAPDNggAAAAAAAAAAAAAAAANtrzNON0u1IEGKmVsm80/Av+BKip0ioeS/4E+Ejs9YPAAAAADwzYIAAAAAAAAAAAAAAAAD+ejNcgNcKjR/ihUx1ikhdz5zmhzvRET3LGd7oOiBlTwAAAAA8M2CAAAAAAAAAAAAAAAAASXG3P6KJjS6e0dzirbso8vRvZKo6zETUsEv7OSP8XekAAAAAPDNggAAAAAAAAAAAAAAAAC5orVpxxvGEB8ISTho2YdOPZJrd7UBj1Bt8TOjLOiEKAAAAADwzYIAAAAAAAAAAAAAAAAAOQR7AqdGyIIMuFLw9JQWtHqsUJD94kHum7SJS9PXkOwAAAAA8M2CAAAAAAAAAAAAAAAAAIosijRx7xSP/+GA6eAjGeV9wJtKDySP+OJr90euE1yQAAAAAPDNggAAAAAAAAAAAAAAAAKlHXWQvwNPeT4Pp1oJDiOpcKwS3d9sho+ha+6pyFwFqAAAAADwzYIAAAAAAAAAAAAAAAABjCjnoL8+FEP0LByZA9PfMLwU1uAX4Cb13rVs83e1UZAAAAAA8M2CAAAAAAAAAAAAAAAAAokhNCZNGq9uAkfKTNoNGr5XmmMoY5poQEmp8OVbit7IAAAAAPDNggAAAAAAAAAABhlbgnAAAAEBa9csgF5/0wxrYM6oVsbM4Yd+/3uVIplS6iLmPOS4xf8oLQLtjKKKIIKmg9Gc/yYm3icZyU7icy9hGjcujenMN"
        )
        .payments(
          listOf(
            StellarPayment.builder()
              .id("755914248193")
              .paymentType(StellarPayment.Type.PAYMENT)
              .sourceAccount("GAJKV32ZXP5QLYHPCMLTV5QCMNJR3W6ZKFP6HMDN67EM2ULDHHDGEZYO")
              .destinationAccount("GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364")
              .amount(
                Amount("9.0000000", "FOO:GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364")
              )
              .build()
          )
        )
        .build()

    val wantEvent =
      TransactionEvent.builder()
        .type(TransactionEvent.Type.TRANSACTION_STATUS_CHANGED)
        .id("ceaa7677-a5a7-434e-b02a-8e0801b3e7bd")
        .status(TransactionEvent.Status.PENDING_RECEIVER)
        .statusChange(
          TransactionEvent.StatusChange(
            TransactionEvent.Status.PENDING_SENDER,
            TransactionEvent.Status.PENDING_RECEIVER
          )
        )
        .sep(TransactionEvent.Sep.SEP_31)
        .kind(TransactionEvent.Kind.RECEIVE)
        .amountExpected(Amount("10", fooAsset))
        .amountIn(Amount("9.0000000", fooAsset))
        .amountOut(Amount("20", barAsset))
        .amountFee(Amount("0.5", fooAsset))
        .quoteId("cef1fc13-3f65-4612-b1f2-502d698c816b")
        .startedAt(startedAtMock)
        .updatedAt(transferReceivedAt)
        .transferReceivedAt(null)
        .message("The incoming payment amount was insufficient! Expected: \"10\", Received: \"9\"")
        .sourceAccount("GAJKV32ZXP5QLYHPCMLTV5QCMNJR3W6ZKFP6HMDN67EM2ULDHHDGEZYO")
        .destinationAccount("GBZ4HPSEHKEEJ6MOZBSVV2B3LE27EZLV6LJY55G47V7BGBODWUXQM364")
        .creator(
          StellarId.builder()
            .account("GBE4B7KE62NUBFLYT3BIG4OP5DAXBQX2GSZZOVAYXQKJKIU7P6V2R2N4")
            .build()
        )
        .customers(
          Customers(
            StellarId.builder().id(senderId).build(),
            StellarId.builder().id(receiverId).build()
          )
        )
        .stellarTransactions(listOf(stellarTransaction))
        .build()

    val slotEvent = slot<TransactionEvent>()
    every { eventPublishService.publish(capture(slotEvent)) } just Runs

    paymentOperationToEventListener.onReceived(p)
    verify(exactly = 1) {
      transactionStore.findByStellarMemo("OWI3OGYwZmEtOTNmOS00MTk4LThkOTMtZTc2ZmQwODQ=")
    }
    verify(exactly = 1) { eventPublishService.publish(any()) }

    wantEvent.eventId = slotEvent.captured.eventId
    assertEquals(wantEvent, slotEvent.captured)

    // wantSep31Tx
    val wantSep31Tx = gson.fromJson(gson.toJson(sep31TxMock), JdbcSep31Transaction::class.java)
    wantSep31Tx.status = TransactionEvent.Status.PENDING_RECEIVER.status
    wantSep31Tx.stellarTransactionId =
      "1ad62e48724426be96cf2cdb65d5dacb8fac2e403e50bedb717bfc8eaf05af30"
    wantSep31Tx.transferReceivedAt = null
    wantSep31Tx.updatedAt = transferReceivedAt
    wantSep31Tx.stellarTransactions = listOf(stellarTransaction)

    assertEquals(wantSep31Tx, slotTx.captured)
  }
}
