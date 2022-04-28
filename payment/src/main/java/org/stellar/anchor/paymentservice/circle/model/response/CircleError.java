package org.stellar.anchor.paymentservice.circle.model.response;

import lombok.Data;

@Data
public class CircleError {
  Integer code;
  String message;
}
