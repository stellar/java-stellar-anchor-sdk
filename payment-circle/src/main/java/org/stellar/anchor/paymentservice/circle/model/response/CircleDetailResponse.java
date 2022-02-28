package org.stellar.anchor.paymentservice.circle.model.response;

import lombok.Data;

@Data
public class CircleDetailResponse<T> {
  T data;
}
