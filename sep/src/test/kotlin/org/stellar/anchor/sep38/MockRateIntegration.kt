package org.stellar.anchor.sep38

import org.stellar.anchor.api.callback.GetRateRequest
import org.stellar.anchor.api.callback.GetRateResponse
import org.stellar.anchor.api.callback.RateIntegration

class MockRateIntegration : RateIntegration {
  override fun getRate(request: GetRateRequest?): GetRateResponse {
    TODO("Not implemented! Use it with mockk in your tests")
  }
}
