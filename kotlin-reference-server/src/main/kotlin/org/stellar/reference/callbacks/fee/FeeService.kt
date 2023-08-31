package org.stellar.reference.callbacks.fee

import java.math.BigDecimal
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.api.callback.GetFeeResponse
import org.stellar.anchor.api.exception.UnprocessableEntityException
import org.stellar.anchor.api.shared.Amount
import org.stellar.reference.callbacks.BadRequestException
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
      throw BadRequestException("Send asset must be provided")
    }

    if (request.receiveAsset == null) {
      throw BadRequestException("Receive asset must be provided")
    }

    if (request.clientId == null) {
      throw BadRequestException("Client id must be provided")
    }

    if (request.sendAmount == null && request.receiveAmount == null) {
      throw BadRequestException("Either send or receive amount must be provided")
    }

    if (request.senderId == null) {
      throw BadRequestException("Sender id must be provided")
    } else {
      customerRepository.get(request.senderId)
        ?: throw UnprocessableEntityException("Sender ${request.senderId} not found")
    }

    if (request.receiverId == null) {
      throw BadRequestException("Receiver id must be provided")
    } else {
      customerRepository.get(request.receiverId)
        ?: throw UnprocessableEntityException("Receiver ${request.receiverId} not found")
    }
  }
}
