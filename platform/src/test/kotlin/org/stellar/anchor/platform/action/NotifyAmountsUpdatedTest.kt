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
import org.stellar.anchor.api.rpc.action.NotifyAmountsUpdatedRequest
import org.stellar.anchor.api.sep.SepTransactionStatus.INCOMPLETE
import org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.horizon.Horizon
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.sep24.Sep24TransactionStore
import org.stellar.anchor.sep31.Sep31TransactionStore
import org.stellar.anchor.util.GsonUtils

class NotifyAmountsUpdatedTest {

  companion object {
    private val GSON = GsonUtils.getInstance()
    private const val TX_ID = "testId"
    private const val FIAT_USD = "iso4217:USD"
    private const val STELLAR_USDC =
      "stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN"
    private const val FIAT_USD_CODE = "USD"
  }

  @MockK(relaxed = true) private lateinit var txn24Store: Sep24TransactionStore

  @MockK(relaxed = true) private lateinit var txn31Store: Sep31TransactionStore

  @MockK(relaxed = true) private lateinit var horizon: Horizon

  @MockK(relaxed = true) private lateinit var validator: Validator

  @MockK(relaxed = true) private lateinit var assetService: AssetService

  private lateinit var handler: NotifyAmountsUpdatedHandler

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    this.assetService = DefaultAssetService.fromJsonResource("test_assets.json")
    this.handler =
      NotifyAmountsUpdatedHandler(txn24Store, txn31Store, validator, horizon, assetService)
  }

  @Test
  fun test_handle_unsupportedProtocol() {
    val request = NotifyAmountsUpdatedRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.transferReceivedAt = Instant.now()
    val spyTxn24 = spyk(txn24)

    every { txn24Store.findByTransactionId(TX_ID) } returns spyTxn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { spyTxn24.protocol } returns "100"

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals("Protocol[100] is not supported by action[notify_amounts_updated]", ex.message)
  }

  @Test
  fun test_handle_unsupportedStatus() {
    val request = NotifyAmountsUpdatedRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = INCOMPLETE.toString()
    txn24.transferReceivedAt = Instant.now()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Action[notify_amounts_updated] is not supported for status[incomplete]",
      ex.message
    )
  }

  @Test
  fun test_handle_transferNotReceived() {
    val request = NotifyAmountsUpdatedRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null

    val ex = assertThrows<InvalidRequestException> { handler.handle(request) }
    assertEquals(
      "Action[notify_amounts_updated] is not supported for status[pending_anchor]",
      ex.message
    )
  }

  @Test
  fun test_handle_invalidRequest() {
    val request = NotifyAmountsUpdatedRequest.builder().transactionId(TX_ID).build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.transferReceivedAt = Instant.now()

    val violation1: ConstraintViolation<NotifyAmountsUpdatedRequest> = mockk()
    val violation2: ConstraintViolation<NotifyAmountsUpdatedRequest> = mockk()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { violation1.message } returns "violation error message 1"
    every { violation2.message } returns "violation error message 2"
    every { validator.validate(request) } returns setOf(violation1, violation2)

    val ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("violation error message 1\n" + "violation error message 2", ex.message)
  }

  @Test
  fun test_handle_invalidAmounts() {
    val request =
      NotifyAmountsUpdatedRequest.builder()
        .transactionId(TX_ID)
        .amountOut("1")
        .amountFee("1")
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountFeeAsset = STELLAR_USDC
    txn24.transferReceivedAt = Instant.now()
    val sep24TxnCapture = slot<JdbcSep24Transaction>()

    every { txn24Store.findByTransactionId(TX_ID) } returns txn24
    every { txn31Store.findByTransactionId(any()) } returns null
    every { txn24Store.save(capture(sep24TxnCapture)) } returns null

    request.amountOut = "-1"
    var ex = assertThrows<InvalidParamsException> { handler.handle(request) }
    assertEquals("amount_out.amount should be positive", ex.message)
    request.amountOut = "1"

    request.amountFee = "-1"
    ex = assertThrows { handler.handle(request) }
    assertEquals("amount_fee.amount should be non-negative", ex.message)
    request.amountFee = "1"
  }

  @Test
  fun test_handle_ok() {
    val transferReceivedAt = Instant.now()
    val request =
      NotifyAmountsUpdatedRequest.builder()
        .transactionId(TX_ID)
        .amountOut("0.9")
        .amountFee("0.1")
        .build()
    val txn24 = JdbcSep24Transaction()
    txn24.status = PENDING_ANCHOR.toString()
    txn24.kind = DEPOSIT.kind
    txn24.requestAssetCode = FIAT_USD_CODE
    txn24.amountOutAsset = STELLAR_USDC
    txn24.amountOut = "1.8"
    txn24.amountFeeAsset = STELLAR_USDC
    txn24.amountFee = "0.2"
    txn24.transferReceivedAt = transferReceivedAt
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
    expectedSep24Txn.status = PENDING_ANCHOR.toString()
    expectedSep24Txn.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedSep24Txn.requestAssetCode = FIAT_USD_CODE
    expectedSep24Txn.amountOutAsset = STELLAR_USDC
    expectedSep24Txn.amountOut = "0.9"
    expectedSep24Txn.amountFeeAsset = STELLAR_USDC
    expectedSep24Txn.amountFee = "0.1"
    expectedSep24Txn.transferReceivedAt = transferReceivedAt

    JSONAssert.assertEquals(
      GSON.toJson(expectedSep24Txn),
      GSON.toJson(sep24TxnCapture.captured),
      JSONCompareMode.STRICT
    )

    val expectedResponse = GetTransactionResponse()
    expectedResponse.sep = SEP_24
    expectedResponse.kind = DEPOSIT
    expectedResponse.status = PENDING_ANCHOR
    expectedResponse.amountExpected = Amount(null, FIAT_USD)
    expectedResponse.amountOut = Amount("0.9", STELLAR_USDC)
    expectedResponse.amountFee = Amount("0.1", STELLAR_USDC)
    expectedResponse.updatedAt = sep24TxnCapture.captured.updatedAt
    expectedResponse.transferReceivedAt = transferReceivedAt

    JSONAssert.assertEquals(
      GSON.toJson(expectedResponse),
      GSON.toJson(response),
      JSONCompareMode.STRICT
    )

    assertTrue(expectedSep24Txn.updatedAt.isAfter(startDate))
    assertTrue(expectedSep24Txn.updatedAt.isBefore(endDate))
  }
}
