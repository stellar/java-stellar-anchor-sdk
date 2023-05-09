package org.stellar.anchor.platform.service

import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import java.util.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest
import org.stellar.anchor.api.custody.CreateTransactionPaymentResponse
import org.stellar.anchor.api.exception.CustodyException
import org.stellar.anchor.api.exception.InvalidConfigException
import org.stellar.anchor.api.exception.custody.CustodyBadRequestException
import org.stellar.anchor.api.exception.custody.CustodyNotFoundException
import org.stellar.anchor.api.exception.custody.CustodyServiceUnavailableException
import org.stellar.anchor.api.exception.custody.CustodyTooManyRequestsException
import org.stellar.anchor.custody.CustodyService
import org.stellar.anchor.platform.apiclient.CustodyApiClient
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.platform.data.JdbcSep31Transaction
import org.stellar.anchor.util.FileUtil
import org.stellar.anchor.util.GsonUtils

class CustodyServiceTest {

  companion object {
    private val gson = GsonUtils.getInstance()
    private val TXN_ID = "1"
    private val REQUEST_BODY = "{}"
  }

  @MockK(relaxed = true) private lateinit var custodyApiClient: CustodyApiClient
  private lateinit var custodyService: CustodyService

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    custodyService = CustodyServiceImpl(Optional.of(custodyApiClient))
  }

  @Test
  fun test_createTransaction_sep24Deposit() {
    val txn =
      gson.fromJson(
        FileUtil.getResourceFileAsString("service/custodyTransaction/sep24_deposit_entity.json"),
        JdbcSep24Transaction::class.java
      )
    val requestCapture = slot<CreateCustodyTransactionRequest>()

    every { custodyApiClient.createTransaction(capture(requestCapture)) } just Runs

    custodyService.createTransaction(txn)

    JSONAssert.assertEquals(
      FileUtil.getResourceFileAsString("service/custodyTransaction/sep24_deposit_request.json"),
      gson.toJson(requestCapture.captured),
      JSONCompareMode.STRICT
    )
  }

  @Test
  fun test_createTransaction_sep24Withdrawal() {
    val txn =
      gson.fromJson(
        FileUtil.getResourceFileAsString("service/custodyTransaction/sep24_withdrawal_entity.json"),
        JdbcSep24Transaction::class.java
      )
    val requestCapture = slot<CreateCustodyTransactionRequest>()

    every { custodyApiClient.createTransaction(capture(requestCapture)) } just Runs

    custodyService.createTransaction(txn)

    JSONAssert.assertEquals(
      FileUtil.getResourceFileAsString("service/custodyTransaction/sep24_withdrawal_request.json"),
      gson.toJson(requestCapture.captured),
      JSONCompareMode.STRICT
    )
  }

  @Test
  fun test_createTransaction_sep31() {
    val txn =
      gson.fromJson(
        FileUtil.getResourceFileAsString("service/custodyTransaction/sep31_entity.json"),
        JdbcSep31Transaction::class.java
      )
    val requestCapture = slot<CreateCustodyTransactionRequest>()

    every { custodyApiClient.createTransaction(capture(requestCapture)) } just Runs

    custodyService.createTransaction(txn)

    JSONAssert.assertEquals(
      FileUtil.getResourceFileAsString("service/custodyTransaction/sep31_request.json"),
      gson.toJson(requestCapture.captured),
      JSONCompareMode.STRICT
    )
  }

  @Test
  fun test_createTransactionPayment_custody_integration_not_enabled() {
    custodyService = CustodyServiceImpl(Optional.empty())

    val ex =
      assertThrows<InvalidConfigException> {
        custodyService.createTransactionPayment(TXN_ID, REQUEST_BODY)
      }
    Assertions.assertEquals("Integration with custody services is not enabled", ex.message)
    verify(exactly = 0) { custodyApiClient.createTransactionPayment(TXN_ID, REQUEST_BODY) }
  }

  @Test
  fun test_createTransactionPayment_custody_integration_enabled() {
    every { custodyApiClient.createTransactionPayment(TXN_ID, REQUEST_BODY) } returns
      CreateTransactionPaymentResponse(TXN_ID)

    custodyService.createTransactionPayment(TXN_ID, REQUEST_BODY)

    verify(exactly = 1) { custodyApiClient.createTransactionPayment(TXN_ID, REQUEST_BODY) }
  }

  @Test
  fun test_createTransactionPayment_custody_server_unavailable() {
    every { custodyApiClient.createTransactionPayment(TXN_ID, REQUEST_BODY) } throws
      CustodyException("Custody service is unavailable", 503)

    val exception =
      assertThrows<CustodyServiceUnavailableException> {
        custodyService.createTransactionPayment(TXN_ID, REQUEST_BODY)
      }
    Assertions.assertEquals("Custody service is unavailable", exception.message)
  }

  @Test
  fun test_createTransactionPayment_bad_request() {
    every { custodyApiClient.createTransactionPayment(TXN_ID, REQUEST_BODY) } throws
      CustodyException("Bad request", 400)

    val exception =
      assertThrows<CustodyBadRequestException> {
        custodyService.createTransactionPayment(TXN_ID, REQUEST_BODY)
      }
    Assertions.assertEquals("Bad request", exception.message)
  }

  @Test
  fun test_createTransactionPayment_too_many_requests() {
    every { custodyApiClient.createTransactionPayment(TXN_ID, REQUEST_BODY) } throws
      CustodyException("Too many requests", 429)

    val exception =
      assertThrows<CustodyTooManyRequestsException> {
        custodyService.createTransactionPayment(TXN_ID, REQUEST_BODY)
      }
    Assertions.assertEquals("Too many requests", exception.message)
  }

  @Test
  fun test_createTransactionPayment_transaction_not_found() {
    every { custodyApiClient.createTransactionPayment(TXN_ID, REQUEST_BODY) } throws
      CustodyException("Transaction (id=1) is not found", 404)

    val exception =
      assertThrows<CustodyNotFoundException> {
        custodyService.createTransactionPayment(TXN_ID, REQUEST_BODY)
      }
    Assertions.assertEquals("Transaction (id=1) is not found", exception.message)
  }

  @Test
  fun test_createTransactionPayment_unexpected_status_code() {
    every { custodyApiClient.createTransactionPayment(TXN_ID, REQUEST_BODY) } throws
      CustodyException("Forbidden", 403)

    val exception =
      assertThrows<CustodyException> {
        custodyService.createTransactionPayment(TXN_ID, REQUEST_BODY)
      }
    Assertions.assertEquals("Forbidden", exception.rawMessage)
  }
}
