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
  fun test_create_sep24_deposit_pending_user_transfer_start_entity() {
    val txn =
      gson.fromJson(
        FileUtil.getResourceFileAsString(
          "service/custodyTransaction/sep24_deposit_pending_user_transfer_start_entity.json"
        ),
        JdbcSep24Transaction::class.java
      )

    custodyTransactionService.create(txn)

    verify(exactly = 0) { custodyApiClient.createTransaction(any()) }
  }

  @Test
  fun test_create_sep24_withdrawal_pending_anchor_entity() {
    val txn =
      gson.fromJson(
        FileUtil.getResourceFileAsString(
          "service/custodyTransaction/sep24_withdrawal_pending_anchor_entity.json"
        ),
        JdbcSep24Transaction::class.java
      )

    custodyTransactionService.create(txn)

    verify(exactly = 0) { custodyApiClient.createTransaction(any()) }
  }

  @Test
  fun test_create_sep24_deposit_pending_anchor() {
    val txn =
      gson.fromJson(
        FileUtil.getResourceFileAsString(
          "service/custodyTransaction/sep24_deposit_pending_anchor_entity.json"
        ),
        JdbcSep24Transaction::class.java
      )
    val requestCapture = slot<CreateCustodyTransactionRequest>()

    every { custodyApiClient.createTransaction(capture(requestCapture)) } just Runs

    custodyTransactionService.create(txn)

    JSONAssert.assertEquals(
      FileUtil.getResourceFileAsString(
        "service/custodyTransaction/sep24_deposit_pending_anchor_request.json"
      ),
      gson.toJson(requestCapture.captured),
      JSONCompareMode.STRICT
    )
  }

  @Test
  fun test_create_sep24_withdrawal_pending_user_transfer_start() {
    val txn =
      gson.fromJson(
        FileUtil.getResourceFileAsString(
          "service/custodyTransaction/sep24_withdrawal_pending_user_transfer_start_entity.json"
        ),
        JdbcSep24Transaction::class.java
      )
    val requestCapture = slot<CreateCustodyTransactionRequest>()

    every { custodyApiClient.createTransaction(capture(requestCapture)) } just Runs

    custodyTransactionService.create(txn)

    JSONAssert.assertEquals(
      FileUtil.getResourceFileAsString(
        "service/custodyTransaction/sep24_withdrawal_pending_user_transfer_start_request.json"
      ),
      gson.toJson(requestCapture.captured),
      JSONCompareMode.STRICT
    )
  }

  @Test
  fun test_create_sep31_pending_receiver() {
    val txn =
      gson.fromJson(
        FileUtil.getResourceFileAsString(
          "service/custodyTransaction/sep31_pending_receiver_entity.json"
        ),
        JdbcSep31Transaction::class.java
      )

    custodyTransactionService.create(txn)

    verify(exactly = 0) { custodyApiClient.createTransaction(any()) }
  }

  @Test
  fun test_create_sep31_pending_sender() {
    val txn =
      gson.fromJson(
        FileUtil.getResourceFileAsString(
          "service/custodyTransaction/sep31_pending_sender_entity.json"
        ),
        JdbcSep31Transaction::class.java
      )
    val requestCapture = slot<CreateCustodyTransactionRequest>()

    every { custodyApiClient.createTransaction(capture(requestCapture)) } just Runs

    custodyTransactionService.create(txn)

    JSONAssert.assertEquals(
      FileUtil.getResourceFileAsString(
        "service/custodyTransaction/sep31_pending_sender_request.json"
      ),
      gson.toJson(requestCapture.captured),
      JSONCompareMode.STRICT
    )
  }
}
