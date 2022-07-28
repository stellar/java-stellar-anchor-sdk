package org.stellar.anchor.platform.service

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.AnchorException
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.NotFoundException
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.sep31.Sep31TransactionStore
import org.stellar.anchor.sep38.Sep38QuoteStore

class TransactionServiceTest {
  companion object {
    private const val fiatUSD = "iso4217:USD"
    private const val stellarUSDC =
      "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
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
}
