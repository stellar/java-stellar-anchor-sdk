package org.stellar.anchor.paymentservice.stellar.requests;

import lombok.Data;

@Data
public class SubmitTransactionRequest {
  String envelope;

  public SubmitTransactionRequest(String envelope) {}
}
