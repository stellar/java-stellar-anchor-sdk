package org.stellar.anchor.reference.service;

import static org.stellar.anchor.util.MathHelper.decimal;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.callback.GetFeeRequest;
import org.stellar.anchor.api.callback.GetFeeResponse;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.reference.model.Customer;
import org.stellar.anchor.reference.repo.CustomerRepo;

@Service
public class FeeService {
  private final CustomerRepo customerRepo;
  BigDecimal feePercent = decimal("0.02"); // fixed 2% fee.
  BigDecimal feeFixed = decimal("0.1");

  FeeService(CustomerRepo customerRepo) {
    this.customerRepo = customerRepo;
  }

  public GetFeeResponse getFee(GetFeeRequest request) throws BadRequestException {
    if (request.getSendAsset() == null) {
      throw new BadRequestException("send_asset cannot be empty.");
    }

    if (request.getReceiveAsset() == null) {
      throw new BadRequestException("receive_asset cannot be empty.");
    }

    if (request.getClientId() == null) {
      throw new BadRequestException("client_id cannot be empty.");
    }

    if (request.getSendAmount() == null && request.getReceiveAmount() == null) {
      throw new BadRequestException("sender_amount or receiver_amount must be present.");
    }

    if (request.getSenderId() == null) {
      throw new BadRequestException("sender_id cannot be empty.");
    }
    Optional<Customer> maybeSender = customerRepo.findById(request.getSenderId());
    if (maybeSender.isEmpty()) {
      throw new BadRequestException("sender_id was not found.");
    }

    if (request.getReceiverId() == null) {
      throw new BadRequestException("receiver_id cannot be empty.");
    }
    Optional<Customer> maybeReceiver = customerRepo.findById(request.getReceiverId());
    if (maybeReceiver.isEmpty()) {
      throw new BadRequestException("receiver_id was not found.");
    }

    BigDecimal amount = decimal(request.getSendAmount());
    // fee = feeFixed + feePercent * sendAmount
    BigDecimal fee = amount.multiply(feePercent).add(feeFixed);
    return new GetFeeResponse(new Amount(String.valueOf(fee), request.getSendAsset()));
  }
}
