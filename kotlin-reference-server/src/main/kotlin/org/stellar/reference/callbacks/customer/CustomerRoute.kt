package org.stellar.reference.callbacks.customer

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.stellar.anchor.api.callback.GetCustomerRequest
import org.stellar.anchor.api.callback.PutCustomerRequest
import org.stellar.anchor.util.GsonUtils
import org.stellar.reference.callbacks.BadRequestException
import org.stellar.reference.callbacks.NotFoundException
import org.stellar.reference.log

/**
 * Defines the routes related to the customer callback API. See
 * [Customer Callbacks](https://developers.stellar.org/api/anchor-platform/callbacks/customer/).
 *
 * @param customerService the [CustomerService] to use to process the requests.
 */
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
        } catch (e: BadRequestException) {
          call.respond(HttpStatusCode.BadRequest, e)
        } catch (e: NotFoundException) {
          call.respond(HttpStatusCode.NotFound, e)
        } catch (e: Exception) {
          log.error("Unexpected exception", e)
          call.respond(HttpStatusCode.InternalServerError)
        }
      }
      put {
        val request =
          GsonUtils.getInstance().fromJson(call.receive<String>(), PutCustomerRequest::class.java)
        try {
          val response = GsonUtils.getInstance().toJson(customerService.upsertCustomer(request))
          call.respond(response)
        } catch (e: BadRequestException) {
          call.respond(HttpStatusCode.BadRequest, e)
        } catch (e: Exception) {
          call.respond(HttpStatusCode.InternalServerError)
        }
      }
      delete("{id}") {
        val id = call.parameters["id"]!!
        try {
          customerService.deleteCustomer(id)
          call.respond(HttpStatusCode.NoContent)
        } catch (e: NotFoundException) {
          call.respond(HttpStatusCode.NotFound, e)
        } catch (e: Exception) {
          call.respond(HttpStatusCode.InternalServerError)
        }
      }
    }
    route("/invalidate_clabe") {
      get("{id}") {
        val id = call.parameters["id"]!!
        try {
          customerService.invalidateClabe(id)
          call.respond(HttpStatusCode.OK)
        } catch (e: NotFoundException) {
          call.respond(HttpStatusCode.NotFound, e)
        } catch (e: Exception) {
          call.respond(HttpStatusCode.InternalServerError)
        }
      }
    }
  }
}
