package org.stellar.anchor.platform.service

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.util.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest
import org.stellar.anchor.custody.CustodyTransactionService
import org.stellar.anchor.platform.apiclient.CustodyApiClient
import org.stellar.anchor.platform.data.JdbcSep24Transaction
import org.stellar.anchor.platform.data.JdbcSep31Transaction
import org.stellar.anchor.util.FileUtil
import org.stellar.anchor.util.GsonUtils

class CustodyTransactionServiceTest {

  private val gson = GsonUtils.getInstance()

  @MockK(relaxed = true) private lateinit var custodyApiClient: CustodyApiClient
  private lateinit var custodyTransactionService: CustodyTransactionService

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    custodyTransactionService = CustodyTransactionServiceImpl(Optional.of(custodyApiClient))
  }

  @Test
  fun test_create_sep24Deposit() {
    val txn =
      gson.fromJson(
        FileUtil.getResourceFileAsString("service/custodyTransaction/sep24_deposit_entity.json"),
        JdbcSep24Transaction::class.java
      )
    val requestCapture = slot<CreateCustodyTransactionRequest>()

    every { custodyApiClient.createTransaction(capture(requestCapture)) } just Runs

    custodyTransactionService.create(txn)

    JSONAssert.assertEquals(
      FileUtil.getResourceFileAsString("service/custodyTransaction/sep24_deposit_request.json"),
      gson.toJson(requestCapture.captured),
      JSONCompareMode.STRICT
    )
  }

  @Test
  fun test_create_sep24Withdrawal() {
    val txn =
      gson.fromJson(
        FileUtil.getResourceFileAsString("service/custodyTransaction/sep24_withdrawal_entity.json"),
        JdbcSep24Transaction::class.java
      )
    val requestCapture = slot<CreateCustodyTransactionRequest>()

    every { custodyApiClient.createTransaction(capture(requestCapture)) } just Runs

    custodyTransactionService.create(txn)

    JSONAssert.assertEquals(
      FileUtil.getResourceFileAsString("service/custodyTransaction/sep24_withdrawal_request.json"),
      gson.toJson(requestCapture.captured),
      JSONCompareMode.STRICT
    )
  }

  @Test
  fun test_create_sep31() {
    val txn =
      gson.fromJson(
        FileUtil.getResourceFileAsString("service/custodyTransaction/sep31_entity.json"),
        JdbcSep31Transaction::class.java
      )
    val requestCapture = slot<CreateCustodyTransactionRequest>()

    every { custodyApiClient.createTransaction(capture(requestCapture)) } just Runs

    custodyTransactionService.create(txn)

    JSONAssert.assertEquals(
      FileUtil.getResourceFileAsString("service/custodyTransaction/sep31_request.json"),
      gson.toJson(requestCapture.captured),
      JSONCompareMode.STRICT
    )
  }
}
