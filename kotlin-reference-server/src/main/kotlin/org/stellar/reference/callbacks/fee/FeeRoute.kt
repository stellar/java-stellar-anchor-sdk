package org.stellar.reference.callbacks.fee

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.util.GsonUtils
import org.stellar.reference.plugins.AUTH_CONFIG_ENDPOINT

/**
 * Defines the routes related to the fee callback API. See
 * [Fee Callbacks](https://developers.stellar.org/api/anchor-platform/callbacks/fee/).
 *
 * @param feeService the [FeeService] to use to process the requests.
 */
fun Route.fee(feeService: FeeService) {
  authenticate(AUTH_CONFIG_ENDPOINT) {
    get("/fee") {
      val request =
        GetFeeRequest.builder()
          .sendAsset(call.parameters["send_asset"])
          .receiveAsset(call.parameters["receive_asset"])
          .sendAmount(call.parameters["send_amount"])
          .receiveAmount(call.parameters["receive_amount"])
          .clientId(call.parameters["client_id"])
          .senderId(call.parameters["sender_id"])
          .receiverId(call.parameters["receiver_id"])
          .build()
      val response = GsonUtils.getInstance().toJson(feeService.getFee(request))
      call.respond(response)
    }
  }
}
