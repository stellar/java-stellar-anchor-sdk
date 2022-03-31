package org.stellar.anchor.reference.service;

import org.springframework.stereotype.Service;
import org.stellar.anchor.exception.BadRequestException;
import org.stellar.platform.apis.callbacks.requests.GetFeeRequest;
import org.stellar.platform.apis.callbacks.responses.GetFeeResponse;
import org.stellar.platform.apis.shared.Amount;

import java.math.BigDecimal;

@Service
public class FeeService {
  BigDecimal feePercent = new BigDecimal("0.02"); // fixed 2% fee.
  BigDecimal feeFixed = new BigDecimal("0.1");

  public GetFeeResponse getFee(GetFeeRequest request) throws BadRequestException {
    if (request.getSendAmount() == null || request.getSendAsset() == null || request.getReceiveAsset()==null) {
      throw new BadRequestException("Invalid fee request.");
    }
    BigDecimal amount = new BigDecimal(request.getSendAmount());
    // fee = feeFixed + feePercent * sendAmount
    BigDecimal fee = amount.multiply(feePercent).add(feeFixed);
    return new GetFeeResponse(new Amount(String.valueOf(fee), request.getSendAsset()));
  }
}
