package org.stellar.anchor.platform.custody

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.util.*
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest
import org.stellar.anchor.api.exception.FireblocksException
import org.stellar.anchor.api.exception.custody.CustodyBadRequestException
import org.stellar.anchor.api.exception.custody.CustodyNotFoundException
import org.stellar.anchor.api.exception.custody.CustodyServiceUnavailableException
import org.stellar.anchor.api.exception.custody.CustodyTooManyRequestsException
import org.stellar.anchor.platform.data.JdbcCustodyTransaction
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo
import org.stellar.anchor.util.FileUtil.getResourceFileAsString
import org.stellar.anchor.util.GsonUtils

class CustodyTransactionServiceTest {

  companion object {
    private const val TRANSACTION_ID = "TRANSACTION_ID"
    private const val REQUEST_BODY = "REQUEST_BODY"
  }

  private val gson = GsonUtils.getInstance()

  @MockK(relaxed = true) private lateinit var custodyTransactionRepo: JdbcCustodyTransactionRepo
  @MockK(relaxed = true) private lateinit var paymentService: PaymentService

  private lateinit var custodyTransactionService: CustodyTransactionService

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    custodyTransactionService = CustodyTransactionService(custodyTransactionRepo, paymentService)
  }

  @Test
  fun test_create_success() {
    val request =
      gson.fromJson(
        getResourceFileAsString("custody/api/transaction/create_custody_transaction_request.json"),
        CreateCustodyTransactionRequest::class.java
      )
    val entityJson =
      getResourceFileAsString("custody/api/transaction/create_custody_transaction_entity.json")
    val entityCapture = slot<JdbcCustodyTransaction>()

    every { custodyTransactionRepo.save(capture(entityCapture)) } returns null

    custodyTransactionService.create(request)

    val actualCustodyTransaction = entityCapture.captured
    assertTrue(!Instant.now().isBefore(actualCustodyTransaction.createdAt))
    actualCustodyTransaction.createdAt = null
    JSONAssert.assertEquals(entityJson, gson.toJson(entityCapture.captured), JSONCompareMode.STRICT)
  }

  @Test
  fun test_createPayment_transaction_does_not_exist() {
    every { custodyTransactionRepo.findById(TRANSACTION_ID) } returns Optional.empty()

    val exception =
      assertThrows<CustodyNotFoundException> {
        custodyTransactionService.createPayment(TRANSACTION_ID, REQUEST_BODY)
      }
    Assertions.assertEquals("Transaction (id=TRANSACTION_ID) is not found", exception.message)
    verify(exactly = 0) { paymentService.createTransactionPayment(any(), any()) }
  }

  @Test
  fun test_createPayment_transaction_exists() {
    val transaction = JdbcCustodyTransaction()
    every { custodyTransactionRepo.findById(TRANSACTION_ID) } returns Optional.of(transaction)
    every { custodyTransactionRepo.save(any()) } returns null

    custodyTransactionService.createPayment(TRANSACTION_ID, REQUEST_BODY)
    verify(exactly = 1) { paymentService.createTransactionPayment(transaction, REQUEST_BODY) }
  }

  @Test
  fun test_createPayment_bad_request() {
    val transaction = JdbcCustodyTransaction()
    every { custodyTransactionRepo.findById(TRANSACTION_ID) } returns Optional.of(transaction)
    every { custodyTransactionRepo.save(any()) } returns null
    every { paymentService.createTransactionPayment(transaction, REQUEST_BODY) } throws
      FireblocksException("Bad request", 400)

    val ex =
      assertThrows<CustodyBadRequestException> {
        custodyTransactionService.createPayment(TRANSACTION_ID, REQUEST_BODY)
      }
    Assertions.assertEquals("Bad request", ex.message)
  }

  @Test
  fun test_createPayment_too_many_requests() {
    val transaction = JdbcCustodyTransaction()
    every { custodyTransactionRepo.findById(TRANSACTION_ID) } returns Optional.of(transaction)
    every { custodyTransactionRepo.save(any()) } returns null
    every { paymentService.createTransactionPayment(transaction, REQUEST_BODY) } throws
      FireblocksException("Too many requests", 429)

    val ex =
      assertThrows<CustodyTooManyRequestsException> {
        custodyTransactionService.createPayment(TRANSACTION_ID, REQUEST_BODY)
      }
    Assertions.assertEquals("Too many requests", ex.message)
  }

  @Test
  fun test_createPayment_service_unavailable() {
    val transaction = JdbcCustodyTransaction()
    every { custodyTransactionRepo.findById(TRANSACTION_ID) } returns Optional.of(transaction)
    every { custodyTransactionRepo.save(any()) } returns null
    every { paymentService.createTransactionPayment(transaction, REQUEST_BODY) } throws
      FireblocksException("Service unavailable", 503)

    val ex =
      assertThrows<CustodyServiceUnavailableException> {
        custodyTransactionService.createPayment(TRANSACTION_ID, REQUEST_BODY)
      }
    Assertions.assertEquals("Service unavailable", ex.message)
  }

  @Test
  fun test_createPayment_unexpected_status_code() {
    val transaction = JdbcCustodyTransaction()
    every { custodyTransactionRepo.findById(TRANSACTION_ID) } returns Optional.of(transaction)
    every { custodyTransactionRepo.save(any()) } returns null
    every { paymentService.createTransactionPayment(transaction, REQUEST_BODY) } throws
      FireblocksException("Forbidden", 403)

    val ex =
      assertThrows<FireblocksException> {
        custodyTransactionService.createPayment(TRANSACTION_ID, REQUEST_BODY)
      }
    Assertions.assertEquals(
      "Fireblocks API returned an error. HTTP status[403], response[Forbidden]",
      ex.message
    )
  }
}
