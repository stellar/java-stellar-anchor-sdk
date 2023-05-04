package org.stellar.anchor.platform.custody

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import java.time.Instant
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest
import org.stellar.anchor.platform.data.JdbcCustodyTransaction
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo
import org.stellar.anchor.util.FileUtil.getResourceFileAsString
import org.stellar.anchor.util.GsonUtils

class CustodyTransactionServiceTest {

  private val gson = GsonUtils.getInstance()

  @MockK(relaxed = true) private lateinit var custodyTransactionRepo: JdbcCustodyTransactionRepo

  private lateinit var custodyTransactionService: CustodyTransactionService

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    custodyTransactionService = CustodyTransactionService(custodyTransactionRepo)
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
    assertTrue(Instant.now().isAfter(actualCustodyTransaction.createdAt))
    actualCustodyTransaction.createdAt = null
    JSONAssert.assertEquals(entityJson, gson.toJson(entityCapture.captured), JSONCompareMode.STRICT)
  }
}
