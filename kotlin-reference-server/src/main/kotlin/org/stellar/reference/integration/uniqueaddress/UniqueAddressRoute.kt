package org.stellar.reference.integration.uniqueaddress

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.stellar.anchor.api.callback.GetUniqueAddressRequest

fun Route.uniqueAddress(uniqueAddressService: UniqueAddressService) {
  get("/unique_address") {
    val request = call.receive<GetUniqueAddressRequest>()
    call.respond(uniqueAddressService.getUniqueAddress(request))
  }
}
