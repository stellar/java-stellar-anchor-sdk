package org.stellar.anchor.sep38

import org.stellar.anchor.integration.rate.GetRateRequest
import org.stellar.anchor.integration.rate.GetRateResponse
import org.stellar.anchor.integration.rate.RateIntegration

class MockRateIntegration : RateIntegration {
  override fun getRate(request: GetRateRequest?): GetRateResponse {
    TODO("Not yet implemented, use it with mockk in your tests")
  }
}
