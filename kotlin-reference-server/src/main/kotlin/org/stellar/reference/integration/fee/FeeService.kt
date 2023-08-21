package org.stellar.reference.integration.fee

import java.math.BigDecimal
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.api.callback.GetFeeResponse
import org.stellar.anchor.api.shared.Amount
import org.stellar.reference.dao.CustomerRepository

class FeeService(private val customerRepository: CustomerRepository) {
  fun getFee(request: GetFeeRequest): GetFeeResponse {
    validateRequest(request)
    val amount = request.sendAmount?.let { BigDecimal(it) }
    val fee = amount?.multiply(BigDecimal("0.02"))?.add(BigDecimal("0.1"))

    return GetFeeResponse(Amount(fee!!.toString(), request.sendAsset))
  }

  private fun validateRequest(request: GetFeeRequest) {
    if (request.sendAsset == null) {
      throw RuntimeException("Send asset must be provided")
    }

    if (request.receiveAsset == null) {
      throw RuntimeException("Receive asset must be provided")
    }

    if (request.clientId == null) {
      throw RuntimeException("Client id must be provided")
    }

    if (request.sendAmount == null && request.receiveAmount == null) {
      throw RuntimeException("Either send or receive amount must be provided")
    }

    if (request.senderId == null) {
      throw RuntimeException("Sender id must be provided")
    } else {
      customerRepository.get(request.senderId)
        ?: throw RuntimeException("Sender ${request.senderId} not found")
    }

    if (request.receiverId == null) {
      throw RuntimeException("Receiver id must be provided")
    } else {
      customerRepository.get(request.receiverId)
        ?: throw RuntimeException("Receiver ${request.receiverId} not found")
    }
  }
}
