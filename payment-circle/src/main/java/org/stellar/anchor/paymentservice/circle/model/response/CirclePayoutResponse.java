package org.stellar.anchor.paymentservice.circle.model.response;

import lombok.Data;
import org.stellar.anchor.paymentservice.circle.model.CirclePayout;

@Data
public class CirclePayoutResponse {
  CirclePayout data;
}
