package org.stellar.anchor.platform.payment.observer.circle.model.response;

import lombok.Data;

@Data
public class CircleError {
  Integer code;
  String message;
}
