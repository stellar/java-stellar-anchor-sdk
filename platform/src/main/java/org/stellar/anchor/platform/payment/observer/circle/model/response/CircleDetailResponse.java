package org.stellar.anchor.platform.payment.observer.circle.model.response;

import lombok.Data;

@Data
public class CircleDetailResponse<T> {
  T data;
}
