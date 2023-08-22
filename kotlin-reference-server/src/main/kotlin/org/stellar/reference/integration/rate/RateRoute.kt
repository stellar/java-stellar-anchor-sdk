package org.stellar.reference.integration.rate

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.stellar.anchor.api.callback.GetRateRequest
import org.stellar.anchor.util.GsonUtils

fun Route.rate(rateService: RateService) {
  authenticate("integration-auth") {
    get("/rate") {
      val request =
        GetRateRequest.builder()
          .type(call.parameters["type"]?.let { GetRateRequest.Type.valueOf(it) })
          .sellAsset(call.parameters["sell_asset"])
          .sellAmount(call.parameters["sell_amount"])
          .sellDeliveryMethod(call.parameters["sell_delivery_method"])
          .buyAsset(call.parameters["buy_asset"])
          .buyAmount(call.parameters["buy_amount"])
          .buyDeliveryMethod(call.parameters["buy_delivery_method"])
          .countryCode(call.parameters["country_code"])
          .expireAfter(call.parameters["expire_after"])
          .clientId(call.parameters["client_id"])
          .id(call.parameters["id"])
          .build()
      try {
        val response = GsonUtils.getInstance().toJson(rateService.getRate(request))
        call.respond(response)
      } catch (e: Exception) {
        call.respond(
          HttpStatusCode.BadRequest,
          e.message ?: "Error retrieving rate",
        )
      }
    }
  }
}
