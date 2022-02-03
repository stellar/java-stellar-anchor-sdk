package org.stellar.anchor.paymentservice.circle.model.response;

import lombok.Data;
import org.stellar.anchor.paymentservice.circle.model.CircleWallet;

@Data
public class CircleWalletResponse {
  CircleWallet data;
}
