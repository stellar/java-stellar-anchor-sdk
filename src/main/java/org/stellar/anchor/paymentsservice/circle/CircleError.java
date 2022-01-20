package org.stellar.anchor.paymentsservice.circle;

import lombok.Data;

@Data
public class CircleError {
    Integer code;
    String message;
}
