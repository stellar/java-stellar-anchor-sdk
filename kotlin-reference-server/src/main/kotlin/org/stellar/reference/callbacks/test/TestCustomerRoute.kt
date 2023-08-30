package org.stellar.reference.callbacks.test

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.stellar.reference.callbacks.customer.CustomerService
import org.stellar.reference.plugins.AUTH_CONFIG_ENDPOINT

fun Route.testCustomer(customerService: CustomerService) {
  authenticate(AUTH_CONFIG_ENDPOINT) {
    route("/invalidate_clabe") {
      get("{id}") {
        val id = call.parameters["id"]!!
        customerService.invalidateClabe(id)
        call.respond(HttpStatusCode.OK)
      }
    }
  }
}
