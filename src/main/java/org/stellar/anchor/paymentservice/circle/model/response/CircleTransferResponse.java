package org.stellar.anchor.paymentservice.circle.model.response;

import lombok.Data;
import org.stellar.anchor.paymentservice.circle.model.CircleTransfer;

@Data
public class CircleTransferResponse {
    CircleTransfer data;
}
