package org.stellar.anchor.paymentsservice.circle.model.response;

import lombok.Data;
import org.stellar.anchor.paymentsservice.circle.model.CircleWallet;

@Data
public class CircleWalletResponse {
    CircleWallet data;
}
