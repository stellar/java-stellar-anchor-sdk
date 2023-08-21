package org.stellar.reference.integration.rate

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.stellar.anchor.api.callback.GetRateRequest

fun Route.rate(rateService: RateService) {
  get("/rate") {
    val request = call.receive<GetRateRequest>()
    call.respond(rateService.getRate(request))
  }
}
