package org.stellar.reference.integration.fee

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.util.GsonUtils

fun Route.fee(feeService: FeeService) {
  authenticate("integration-auth") {
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
