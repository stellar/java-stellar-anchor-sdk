package org.stellar.anchor.sep31

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import java.time.Instant
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.platform.GetTransactionResponse
import org.stellar.anchor.api.sep.AssetInfo
import org.stellar.anchor.api.sep.sep31.Sep31GetTransactionResponse
import org.stellar.anchor.api.sep.sep31.Sep31GetTransactionResponse.Sep31RefundPayment
import org.stellar.anchor.api.shared.*
import org.stellar.anchor.api.shared.RefundPayment
import org.stellar.anchor.event.models.TransactionEvent

class Sep31TransactionTest {
  companion object {
    private const val fiatUSD = "iso4217:USD"
    private const val stellarUSDC =
      "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    private const val TEST_ACCOUNT = "GCHLHDBOKG2JWMJQBTLSL5XG6NO7ESXI2TAQKZXCXWXB5WI2X6W233PR"
    private const val TEST_MEMO = "test memo"
  }

  @MockK(relaxed = true) private lateinit var sep31TransactionStore: Sep31TransactionStore
  private lateinit var sep31Transaction: Sep31Transaction
  private lateinit var stellarTransaction: StellarTransaction

  private val txId = "a4baff5f-778c-43d6-bbef-3e9fb41d096e"

  // mock time
  private val mockStartedAt = Instant.now().minusSeconds(180)
  private val mockUpdatedAt = mockStartedAt.plusSeconds(60)
  private val mockTransferReceivedAt = mockUpdatedAt.plusSeconds(60)
  private val mockCompletedAt = mockTransferReceivedAt.plusSeconds(60)

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    // Mock the store
    every { sep31TransactionStore.newTransaction() } returns PojoSep31Transaction()
    every { sep31TransactionStore.newRefunds() } returns PojoSep31Refunds()
    every { sep31TransactionStore.newRefundPayment() } answers { PojoSep31RefundPayment() }

    // Mock the store
    every { sep31TransactionStore.newTransaction() } returns PojoSep31Transaction()
    every { sep31TransactionStore.newRefunds() } returns PojoSep31Refunds()
    every { sep31TransactionStore.newRefundPayment() } answers { PojoSep31RefundPayment() }

    // mock refunds
    val mockRefundPayment1 =
      RefundPaymentBuilder(sep31TransactionStore).id("1111").amount("50.0000").fee("4.0000").build()
    val mockRefundPayment2 =
      RefundPaymentBuilder(sep31TransactionStore).id("2222").amount("40.0000").fee("4.0000").build()
    val mockRefunds =
      RefundsBuilder(sep31TransactionStore)
        .amountRefunded("90.0000")
        .amountFee("8.0000")
        .payments(listOf(mockRefundPayment1, mockRefundPayment2))
        .build()

    // mock missing SEP-31 "transaction.fields"
    val mockMissingFields = AssetInfo.Sep31TxnFieldSpecs()
    mockMissingFields.transaction =
      mapOf(
        "receiver_account_number" to
          AssetInfo.Sep31TxnFieldSpec("bank account number of the destination", null, false),
      )

    stellarTransaction =
      StellarTransaction.builder()
        .id("2b862ac297c93e2db43fc58d407cc477396212bce5e6d5f61789f963d5a11300")
        .memo("my-memo")
        .memoType("text")
        .createdAt(mockTransferReceivedAt)
        .envelope("here_comes_the_envelope")
        .payments(
          listOf(
            StellarPayment.builder()
              .id("4609238642995201")
              .amount(Amount("100.0000", fiatUSD))
              .paymentType(StellarPayment.Type.PAYMENT)
              .sourceAccount("GAS4OW4HKJCC2D6VWUHVFR3MJRRVQBXBFQ3LCZJXBR7TWOOBJWE4SRWZ")
              .destinationAccount("GBQC7NCZMQIPWN6ASUJYIDKDPRK34IOIZNQE5WOHPQH536VMOMQVJTN7")
              .build()
          )
        )
        .build()

    // mock the database SEP-31 transaction
    sep31Transaction =
      Sep31TransactionBuilder(sep31TransactionStore)
        .id(txId)
        .status(TransactionEvent.Status.PENDING_RECEIVER.status)
        .statusEta(120)
        .amountExpected("100")
        .amountIn("100.0000")
        .amountInAsset(fiatUSD)
        .amountOut("98.0000000")
        .amountOutAsset(stellarUSDC)
        .amountFee("2.0000")
        .amountFeeAsset(fiatUSD)
        .stellarAccountId(TEST_ACCOUNT)
        .stellarMemo(TEST_MEMO)
        .stellarMemoType("text")
        .startedAt(mockStartedAt)
        .updatedAt(mockUpdatedAt)
        .transferReceivedAt(mockTransferReceivedAt)
        .completedAt(mockCompletedAt)
        .stellarTransactionId("2b862ac297c93e2db43fc58d407cc477396212bce5e6d5f61789f963d5a11300")
        .stellarTransactions(listOf(stellarTransaction))
        .externalTransactionId("external-tx-id")
        .refunded(true)
        .refunds(mockRefunds)
        .requiredInfoMessage("Please don't forget to foo bar")
        .requiredInfoUpdates(mockMissingFields)
        .quoteId("quote-id")
        .clientDomain("test.com")
        .senderId("6c1770b0-0ea4-11ed-861d-0242ac120002")
        .receiverId("31212353-f265-4dba-9eb4-0bbeda3ba7f2")
        .creator(StellarId("141ee445-f32c-4c38-9d25-f4475d6c5558", null))
        .build()
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun `test PlatformApiGetTransactionResponse correctness`() {
    val wantRefunds: Refund =
      Refund.builder()
        .amountRefunded(Amount("90.0000", fiatUSD))
        .amountFee(Amount("8.0000", fiatUSD))
        .payments(
          arrayOf(
            RefundPayment.builder()
              .id("1111")
              .idType(RefundPayment.IdType.STELLAR)
              .amount(Amount("50.0000", fiatUSD))
              .fee(Amount("4.0000", fiatUSD))
              .requestedAt(null)
              .refundedAt(null)
              .build(),
            RefundPayment.builder()
              .id("2222")
              .idType(RefundPayment.IdType.STELLAR)
              .amount(Amount("40.0000", fiatUSD))
              .fee(Amount("4.0000", fiatUSD))
              .requestedAt(null)
              .refundedAt(null)
              .build()
          )
        )
        .build()

    val wantGetTransactionResponse: GetTransactionResponse =
      GetTransactionResponse.builder()
        .id(txId)
        .sep(31)
        .kind("receive")
        .status(TransactionEvent.Status.PENDING_RECEIVER.status)
        .amountExpected(Amount("100", fiatUSD))
        .amountIn(Amount("100.0000", fiatUSD))
        .amountOut(Amount("98.0000000", stellarUSDC))
        .amountFee(Amount("2.0000", fiatUSD))
        .quoteId("quote-id")
        .startedAt(mockStartedAt)
        .updatedAt(mockUpdatedAt)
        .completedAt(mockCompletedAt)
        .transferReceivedAt(mockTransferReceivedAt)
        .message("Please don't forget to foo bar")
        .refunds(wantRefunds)
        .stellarTransactions(listOf(stellarTransaction))
        .externalTransactionId("external-tx-id")
        .customers(
          Customers(
            StellarId("6c1770b0-0ea4-11ed-861d-0242ac120002", null),
            StellarId("31212353-f265-4dba-9eb4-0bbeda3ba7f2", null)
          )
        )
        .creator(StellarId("141ee445-f32c-4c38-9d25-f4475d6c5558", null))
        .build()

    val gotGetTransactionResponse = sep31Transaction.toPlatformApiGetTransactionResponse()
    Assertions.assertEquals(wantGetTransactionResponse, gotGetTransactionResponse)
  }

  @Test
  fun `test Sep31GetTransactionResponse correctness`() {
    val refunds =
      Sep31GetTransactionResponse.Refunds.builder()
        .amountRefunded("90.0000")
        .amountFee("8.0000")
        .payments(
          listOf<Sep31RefundPayment>(
            Sep31RefundPayment.builder().id("1111").amount("50.0000").fee("4.0000").build(),
            Sep31RefundPayment.builder().id("2222").amount("40.0000").fee("4.0000").build()
          )
        )
        .build()

    val requiredInfoUpdates = AssetInfo.Sep31TxnFieldSpecs()
    requiredInfoUpdates.transaction =
      mapOf(
        "receiver_account_number" to
          AssetInfo.Sep31TxnFieldSpec("bank account number of the destination", null, false)
      )

    val wantSep31GetTransactionResponse =
      Sep31GetTransactionResponse.builder()
        .transaction(
          Sep31GetTransactionResponse.TransactionResponse.builder()
            .id(txId)
            .status(TransactionEvent.Status.PENDING_RECEIVER.status)
            .statusEta(120)
            .amountIn("100.0000")
            .amountInAsset(fiatUSD)
            .amountOut("98.0000000")
            .amountOutAsset(stellarUSDC)
            .amountFee("2.0000")
            .amountFeeAsset(fiatUSD)
            .stellarAccountId(TEST_ACCOUNT)
            .stellarMemo(TEST_MEMO)
            .stellarMemoType("text")
            .startedAt(mockStartedAt)
            .completedAt(mockCompletedAt)
            .stellarTransactionId(
              "2b862ac297c93e2db43fc58d407cc477396212bce5e6d5f61789f963d5a11300"
            )
            .externalTransactionId("external-tx-id")
            .refunded(true)
            .refunds(refunds)
            .requiredInfoMessage("Please don't forget to foo bar")
            .requiredInfoUpdates(requiredInfoUpdates)
            .build()
        )
        .build()

    val gotSep31GetTransactionResponse = sep31Transaction.toSep31GetTransactionResponse()
    assertEquals(wantSep31GetTransactionResponse, gotSep31GetTransactionResponse)
  }
}
