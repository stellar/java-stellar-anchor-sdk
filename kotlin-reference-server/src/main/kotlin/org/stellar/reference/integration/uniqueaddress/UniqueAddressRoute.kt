package org.stellar.reference.integration.uniqueaddress

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.stellar.anchor.api.callback.GetUniqueAddressRequest
import org.stellar.anchor.util.GsonUtils
import org.stellar.reference.log

fun Route.uniqueAddress(uniqueAddressService: UniqueAddressService) {
  authenticate("integration-auth") {
    get("/unique_address") {
      val request = GetUniqueAddressRequest(call.parameters["transaction_id"]!!)
      try {
        val response =
          GsonUtils.getInstance().toJson(uniqueAddressService.getUniqueAddress(request))
        call.respond(response)
      } catch (e: Exception) {
        log.error("Unexpected exception", e)
        call.respond(HttpStatusCode.InternalServerError)
      }
    }
  }
}
