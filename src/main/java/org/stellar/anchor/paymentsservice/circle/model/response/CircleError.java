package org.stellar.anchor.paymentsservice.circle.model.response;

import lombok.Data;

@Data
public class CircleError {
    Integer code;
    String message;
}
