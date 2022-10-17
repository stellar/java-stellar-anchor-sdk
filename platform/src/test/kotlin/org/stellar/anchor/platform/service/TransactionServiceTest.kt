package org.stellar.anchor.platform.service

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.stellar.anchor.api.exception.AnchorException
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.NotFoundException
import org.stellar.anchor.api.platform.GetTransactionResponse
import org.stellar.anchor.api.platform.PatchTransactionRequest
import org.stellar.anchor.api.sep.AssetInfo
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.shared.*
import org.stellar.anchor.api.shared.RefundPayment
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.ResourceJsonAssetService
import org.stellar.anchor.event.models.TransactionEvent
import org.stellar.anchor.platform.data.JdbcSep31RefundPayment
import org.stellar.anchor.platform.data.JdbcSep31Refunds
import org.stellar.anchor.platform.data.JdbcSep31Transaction
import org.stellar.anchor.sep31.*
import org.stellar.anchor.sep38.Sep38Quote
import org.stellar.anchor.sep38.Sep38QuoteStore

class TransactionServiceTest {
  companion object {
    private const val fiatUSD = "iso4217:USD"
    private const val stellarUSDC =
      "stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN"
    private const val TEST_ACCOUNT = "GCHLHDBOKG2JWMJQBTLSL5XG6NO7ESXI2TAQKZXCXWXB5WI2X6W233PR"
    private const val TEST_MEMO = "test memo"
  }

  @MockK(relaxed = true) private lateinit var sep38QuoteStore: Sep38QuoteStore
  @MockK(relaxed = true) private lateinit var sep31TransactionStore: Sep31TransactionStore
  @MockK(relaxed = true) private lateinit var assetService: AssetService
  private lateinit var transactionService: TransactionService

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    transactionService = TransactionService(sep38QuoteStore, sep31TransactionStore, assetService)
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun test_getTransaction_failure() {
    // null tx id is rejected with 400
    var ex: AnchorException = assertThrows { transactionService.getTransaction(null) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("transaction id cannot be empty", ex.message)

    // empty tx id is rejected with 400
    ex = assertThrows { transactionService.getTransaction("") }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("transaction id cannot be empty", ex.message)

    // non-existent transaction is rejected with 404
    every { sep31TransactionStore.findByTransactionId(any()) } returns null
    ex = assertThrows { transactionService.getTransaction("not-found-tx-id") }
    assertInstanceOf(NotFoundException::class.java, ex)
    assertEquals("transaction (id=not-found-tx-id) is not found", ex.message)
  }

  @Test
  fun test_getTransaction() {
    val txId = "a4baff5f-778c-43d6-bbef-3e9fb41d096e"

    // Mock the store
    every { sep31TransactionStore.newTransaction() } returns JdbcSep31Transaction()
    every { sep31TransactionStore.newRefunds() } returns JdbcSep31Refunds()
    every { sep31TransactionStore.newRefundPayment() } answers { JdbcSep31RefundPayment() }

    // mock time
    val mockStartedAt = Instant.now().minusSeconds(180)
    val mockUpdatedAt = mockStartedAt.plusSeconds(60)
    val mockTransferReceivedAt = mockUpdatedAt.plusSeconds(60)
    val mockCompletedAt = mockTransferReceivedAt.plusSeconds(60)

    // mock the stellar transaction
    val stellarTransaction =
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

    // mock the database SEP-31 transaction
    val mockSep31Transaction =
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
    every { sep31TransactionStore.findByTransactionId(txId) } returns mockSep31Transaction

    val gotGetTransactionResponse = transactionService.getTransaction(txId)

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
    assertEquals(wantGetTransactionResponse, gotGetTransactionResponse)
  }

  @Test
  fun test_validateAsset_failure() {
    // fails if amount_in.amount is null
    var assetAmount = Amount(null, null)
    var ex =
      assertThrows<AnchorException> { transactionService.validateAsset("amount_in", assetAmount) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("amount_in.amount cannot be empty", ex.message)

    // fails if amount_in.amount is empty
    assetAmount = Amount("", null)
    ex = assertThrows { transactionService.validateAsset("amount_in", assetAmount) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("amount_in.amount cannot be empty", ex.message)

    // fails if amount_in.amount is invalid
    assetAmount = Amount("abc", null)
    ex = assertThrows { transactionService.validateAsset("amount_in", assetAmount) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("amount_in.amount is invalid", ex.message)

    // fails if amount_in.amount is negative
    assetAmount = Amount("-1", null)
    ex = assertThrows { transactionService.validateAsset("amount_in", assetAmount) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("amount_in.amount should be positive", ex.message)

    // fails if amount_in.amount is zero
    assetAmount = Amount("0", null)
    ex = assertThrows { transactionService.validateAsset("amount_in", assetAmount) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("amount_in.amount should be positive", ex.message)

    // fails if amount_in.asset is null
    assetAmount = Amount("10", null)
    ex = assertThrows { transactionService.validateAsset("amount_in", assetAmount) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("amount_in.asset cannot be empty", ex.message)

    // fails if amount_in.asset is empty
    assetAmount = Amount("10", "")
    ex = assertThrows { transactionService.validateAsset("amount_in", assetAmount) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("amount_in.asset cannot be empty", ex.message)

    // fails if listAllAssets is empty
    every { assetService.listAllAssets() } returns listOf()
    val mockAsset = Amount("10", fiatUSD)
    ex = assertThrows { transactionService.validateAsset("amount_in", mockAsset) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("'$fiatUSD' is not a supported asset.", ex.message)

    // fails if listAllAssets does not contain the desired asset
    this.assetService = ResourceJsonAssetService("test_assets.json")
    ex = assertThrows { transactionService.validateAsset("amount_in", mockAsset) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("'$fiatUSD' is not a supported asset.", ex.message)
  }

  @Test
  fun test_validateAsset() {
    this.assetService = ResourceJsonAssetService("test_assets.json")
    transactionService = TransactionService(sep38QuoteStore, sep31TransactionStore, assetService)
    val mockAsset = Amount("10", fiatUSD)
    assertDoesNotThrow { transactionService.validateAsset("amount_in", mockAsset) }
  }

  @ParameterizedTest
  @EnumSource(
    value = SepTransactionStatus::class,
    mode = EnumSource.Mode.EXCLUDE,
    names =
      [
        "PENDING_STELLAR",
        "PENDING_CUSTOMER_INFO_UPDATE",
        "PENDING_RECEIVER",
        "PENDING_EXTERNAL",
        "COMPLETED",
        "REFUNDED",
        "EXPIRED",
        "ERROR"]
  )
  fun test_validateIfStatusIsSupported_failure(sepTxnStatus: SepTransactionStatus) {
    val ex: Exception = assertThrows {
      transactionService.validateIfStatusIsSupported(sepTxnStatus.getName())
    }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("invalid status(${sepTxnStatus.getName()})", ex.message)
  }

  @ParameterizedTest
  @EnumSource(
    value = SepTransactionStatus::class,
    mode = EnumSource.Mode.INCLUDE,
    names =
      [
        "PENDING_STELLAR",
        "PENDING_CUSTOMER_INFO_UPDATE",
        "PENDING_RECEIVER",
        "PENDING_EXTERNAL",
        "COMPLETED",
        "EXPIRED",
        "ERROR"]
  )
  fun test_validateIfStatusIsSupported(sepTxnStatus: SepTransactionStatus) {
    assertDoesNotThrow { transactionService.validateIfStatusIsSupported(sepTxnStatus.getName()) }
  }

  @Test
  fun test_updateSep31Transaction() {
    val txId = "my-tx-id"
    val quoteId = "my-quote-id"

    // mock times
    val mockStartedAt = Instant.now().minusSeconds(180)
    val mockTransferReceivedAt = mockStartedAt.plusSeconds(60)

    val mockRefunds: Refund =
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

    val mockPatchTransactionRequest =
      PatchTransactionRequest.builder()
        .id(txId)
        .status("completed")
        .amountIn(Amount("100", fiatUSD))
        .amountOut(Amount("98", stellarUSDC))
        .amountFee(Amount("2", fiatUSD))
        .transferReceivedAt(mockTransferReceivedAt)
        .message("Remittance was successfully completed.")
        .refunds(mockRefunds)
        .externalTransactionId("external-id")
        .build()

    val mockSep31Transaction = JdbcSep31Transaction()
    mockSep31Transaction.id = txId
    mockSep31Transaction.quoteId = quoteId
    mockSep31Transaction.startedAt = mockStartedAt
    mockSep31Transaction.updatedAt = mockStartedAt

    val mockSep38Quote = mockk<Sep38Quote>(relaxed = true)
    every { mockSep38Quote.id } returns quoteId
    every { mockSep38Quote.sellAmount } returns "100"
    every { mockSep38Quote.sellAsset } returns fiatUSD
    every { mockSep38Quote.buyAmount } returns "98"
    every { mockSep38Quote.buyAsset } returns stellarUSDC
    every { mockSep38Quote.fee.total } returns "2"
    every { mockSep38Quote.fee.asset } returns fiatUSD
    every { sep38QuoteStore.findByQuoteId(quoteId) } returns mockSep38Quote

    this.assetService = ResourceJsonAssetService("test_assets.json")
    transactionService = TransactionService(sep38QuoteStore, sep31TransactionStore, assetService)

    assertEquals(mockSep31Transaction.startedAt, mockSep31Transaction.updatedAt)
    assertNull(mockSep31Transaction.completedAt)
    assertDoesNotThrow {
      transactionService.updateSep31Transaction(mockPatchTransactionRequest, mockSep31Transaction)
    }
    assertTrue(mockSep31Transaction.updatedAt > mockSep31Transaction.startedAt)
    assertTrue(mockSep31Transaction.updatedAt == mockSep31Transaction.completedAt)

    val wantSep31TransactionUpdated = JdbcSep31Transaction()
    wantSep31TransactionUpdated.id = txId
    wantSep31TransactionUpdated.status = "completed"
    wantSep31TransactionUpdated.quoteId = quoteId
    wantSep31TransactionUpdated.startedAt = mockStartedAt
    wantSep31TransactionUpdated.updatedAt = mockSep31Transaction.updatedAt
    wantSep31TransactionUpdated.completedAt = mockSep31Transaction.completedAt
    wantSep31TransactionUpdated.amountIn = "100"
    wantSep31TransactionUpdated.amountInAsset = fiatUSD
    wantSep31TransactionUpdated.amountOut = "98"
    wantSep31TransactionUpdated.amountOutAsset = stellarUSDC
    wantSep31TransactionUpdated.amountFee = "2"
    wantSep31TransactionUpdated.amountFeeAsset = fiatUSD
    wantSep31TransactionUpdated.requiredInfoMessage = "Remittance was successfully completed."
    wantSep31TransactionUpdated.externalTransactionId = "external-id"
    wantSep31TransactionUpdated.transferReceivedAt = mockTransferReceivedAt
    wantSep31TransactionUpdated.refunds = Refunds.of(mockRefunds, sep31TransactionStore)
    assertEquals(wantSep31TransactionUpdated, mockSep31Transaction)
  }
}
