package org.stellar.anchor.paymentservice.circle.model.response;

import java.util.List;
import lombok.Data;
import org.stellar.anchor.paymentservice.circle.model.CircleBlockchainAddress;

@Data
public class CircleBlockchainAddressListResponse {
  List<CircleBlockchainAddress> data;
}
