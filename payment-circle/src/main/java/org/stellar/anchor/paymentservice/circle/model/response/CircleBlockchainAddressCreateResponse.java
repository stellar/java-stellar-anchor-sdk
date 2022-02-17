package org.stellar.anchor.paymentservice.circle.model.response;

import lombok.Data;
import org.stellar.anchor.paymentservice.circle.model.CircleBlockchainAddress;

@Data
public class CircleBlockchainAddressCreateResponse {
  CircleBlockchainAddress data;
}
