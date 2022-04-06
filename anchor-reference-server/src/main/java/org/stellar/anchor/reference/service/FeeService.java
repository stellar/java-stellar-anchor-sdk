package org.stellar.anchor.reference.service;

import static org.stellar.anchor.util.MathHelper.decimal;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.stellar.anchor.exception.BadRequestException;
import org.stellar.platform.apis.callbacks.requests.GetFeeRequest;
import org.stellar.platform.apis.callbacks.responses.GetFeeResponse;
import org.stellar.platform.apis.shared.Amount;

@Service
public class FeeService {
  BigDecimal feePercent = decimal("0.02"); // fixed 2% fee.
  BigDecimal feeFixed = decimal("0.1");

  public GetFeeResponse getFee(GetFeeRequest request) throws BadRequestException {
    if (request.getSendAmount() == null
        || request.getSendAsset() == null
        || request.getReceiveAsset() == null) {
      throw new BadRequestException("Invalid fee request.");
    }

    // TODO: Check if sending and receiving customers are accepted.
    // TODO: Discuss if we want to overload the sending/receiving customer validation in the fee
    // API.

    BigDecimal amount = decimal(request.getSendAmount());
    // fee = feeFixed + feePercent * sendAmount
    BigDecimal fee = amount.multiply(feePercent).add(feeFixed);
    return new GetFeeResponse(new Amount(String.valueOf(fee), request.getSendAsset()));
  }
}
