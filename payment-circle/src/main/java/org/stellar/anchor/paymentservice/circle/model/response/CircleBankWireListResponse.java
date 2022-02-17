package org.stellar.anchor.paymentservice.circle.model.response;

import java.util.List;
import lombok.Data;
import org.stellar.anchor.paymentservice.circle.model.CircleBankWireAccount;

@Data
public class CircleBankWireListResponse {
  List<CircleBankWireAccount> data;
}
