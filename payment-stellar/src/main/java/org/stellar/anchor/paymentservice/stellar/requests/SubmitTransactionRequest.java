package org.stellar.anchor.paymentservice.stellar.requests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubmitTransactionRequest {
  String tx;
}
