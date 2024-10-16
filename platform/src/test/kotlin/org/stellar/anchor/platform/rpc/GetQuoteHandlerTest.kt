package org.stellar.anchor.platform.rpc

import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.platform.GetQuoteResponse
import org.stellar.anchor.api.rpc.method.GetQuoteRpcRequest
import org.stellar.anchor.api.shared.FeeDetails
import org.stellar.anchor.platform.data.JdbcSep38Quote
import org.stellar.anchor.sep38.Sep38QuoteStore

class GetQuoteHandlerTest {
  companion object {
    private const val QUOTE_ID = "testQuoteId"
  }

  @Test
  fun `test get quote`() {
    val sep38QuoteStore = mockk<Sep38QuoteStore>()
    val handler = GetQuoteHandler(sep38QuoteStore)
    val request = GetQuoteRpcRequest(QUOTE_ID)
    val quote = JdbcSep38Quote()
    quote.id = QUOTE_ID
    quote.expiresAt = Instant.now()
    quote.price = "1.0"
    quote.sellAsset = "USD"
    quote.sellAmount = "100.0"
    quote.sellDeliveryMethod = "bank_account"
    quote.buyAsset = "BTC"
    quote.buyAmount = "0.01"
    quote.buyDeliveryMethod = "crypto_wallet"
    quote.fee = FeeDetails("0.01", "USD")
    quote.totalPrice = "1.01"
    quote.creatorAccountId = "testAccountId"
    quote.creatorMemo = "testMemo"
    quote.creatorMemoType = "text"

    every { sep38QuoteStore.findByQuoteId(QUOTE_ID) } returns quote

    val response = handler.handle(request) as GetQuoteResponse

    assertEquals(quote.id, response.id)
    assertEquals(quote.expiresAt, response.expiresAt)
    assertEquals(quote.price, response.price)
    assertEquals(quote.sellAsset, response.sellAsset)
    assertEquals(quote.sellAmount, response.sellAmount)
    assertEquals(quote.sellDeliveryMethod, response.sellDeliveryMethod)
    assertEquals(quote.buyAsset, response.buyAsset)
    assertEquals(quote.buyAmount, response.buyAmount)
    assertEquals(quote.buyDeliveryMethod, response.buyDeliveryMethod)
    assertEquals(quote.fee, response.fee)
    assertEquals(quote.totalPrice, response.totalPrice)
    assertEquals(quote.creatorAccountId, response.creator.account)
    assertEquals(quote.creatorMemo, response.creator.memo)
  }
}
