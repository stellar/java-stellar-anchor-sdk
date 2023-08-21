package org.stellar.reference.integration.rate

import org.stellar.anchor.api.callback.GetRateRequest
import org.stellar.anchor.api.callback.GetRateResponse
import org.stellar.reference.dao.QuoteRepository

class RateService(private val quoteRepository: QuoteRepository) {
  fun getRate(request: GetRateRequest): GetRateResponse {
    val rate =
      when {
        request.id != null -> getRate(request.id)
        request.sellAmount != null && request.buyAmount != null -> {
          validateRequest(request)
          TODO()
        }
        else -> {
          throw RuntimeException("Either id or sell and buy assets must be provided")
        }
      }

    return GetRateResponse(rate)
  }

  private fun getRate(id: String): GetRateResponse.Rate {
    val quote =
      quoteRepository.get(id) ?: throw RuntimeException("Rate with quote id $id not found")

    return GetRateResponse.Rate.builder()
      .id(quote.id)
      .price(quote.price)
      .sellAmount(quote.sellAmount)
      .buyAmount(quote.buyAmount)
      .expiresAt(quote.expiresAt)
      //      .fee(quote.fee)
      .build()
  }

  private fun validateRequest(request: GetRateRequest) {
    if (request.type == null) {
      throw RuntimeException("Type must be provided")
    }

    if (!listOf(GetRateRequest.Type.INDICATIVE, GetRateRequest.Type.FIRM).contains(request.type)) {
      throw RuntimeException("Type must be either indicative or firm")
    }

    if (request.sellAsset == null) {
      throw RuntimeException("Sell asset must be provided")
    }

    if (request.buyAsset == null) {
      throw RuntimeException("Buy asset must be provided")
    }
  }
}
