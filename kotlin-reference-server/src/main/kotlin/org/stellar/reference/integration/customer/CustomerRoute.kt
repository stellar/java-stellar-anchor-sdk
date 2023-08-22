package org.stellar.reference.integration.customer

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.stellar.anchor.api.callback.GetCustomerRequest
import org.stellar.anchor.api.callback.PutCustomerRequest
import org.stellar.anchor.util.GsonUtils

fun Route.customer(customerService: CustomerService) {
  authenticate("integration-auth") {
    route("/customer") {
      get {
        val request =
          GetCustomerRequest.builder()
            .id(call.parameters["id"])
            .account(call.parameters["account"])
            .memo(call.parameters["memo"])
            .memoType(call.parameters["memo_type"])
            .type(call.parameters["type"])
            .lang(call.parameters["lang"])
            .build()
        try {
          val response = GsonUtils.getInstance().toJson(customerService.getCustomer(request))
          call.respond(response)
        } catch (e: Exception) {
          call.respond(
            HttpStatusCode.NotFound,
            ErrorResponse("customer for 'id' '${request.id}' not found", request.id)
          )
        }
      }
      put {
        val request =
          GsonUtils.getInstance().fromJson(call.receive<String>(), PutCustomerRequest::class.java)
        val response = GsonUtils.getInstance().toJson(customerService.upsertCustomer(request))
        call.respond(response)
      }
      delete("{id}") {
        val id = call.parameters["id"]!!
        try {
          customerService.deleteCustomer(id)
          call.respond(HttpStatusCode.NoContent)
        } catch (e: Exception) {
          call.respond(HttpStatusCode.NotFound)
        }
      }
    }
  }
}

@Serializable data class ErrorResponse(val error: String, val id: String)
