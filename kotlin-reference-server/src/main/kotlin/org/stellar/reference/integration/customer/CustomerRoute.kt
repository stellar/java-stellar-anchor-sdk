package org.stellar.reference.integration.customer

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.stellar.anchor.api.callback.GetCustomerRequest
import org.stellar.anchor.api.callback.PutCustomerRequest

fun Route.customer(customerService: CustomerService) {
  route("/customer") {
    get {
      val request = call.receive<GetCustomerRequest>()
      call.respond(customerService.getCustomer(request))
    }
    put {
      val request = call.receive<PutCustomerRequest>()
      call.respond(customerService.upsertCustomer(request))
    }
    delete {
      val id = call.receive<String>()
      customerService.deleteCustomer(id)
    }
  }
}
