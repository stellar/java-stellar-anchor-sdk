package org.stellar.reference.integration.fee

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.stellar.anchor.api.callback.GetFeeRequest

fun Route.fee(feeService: FeeService) {
  get("/fee") {
    val request = call.receive<GetFeeRequest>()
    call.respond(feeService.getFee(request))
  }
}
