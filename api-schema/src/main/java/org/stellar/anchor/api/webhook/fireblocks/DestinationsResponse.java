package org.stellar.anchor.api.webhook.fireblocks;

import lombok.Data;

@Data
public class DestinationsResponse {
  private String amount;
  private TransferPeerPathResponse destination;
  private Float amountUSD;
  private String destinationAddress;
  private String destinationAddressDescription;
  private AmlScreeningResult amlScreeningResult;
  private String customerRefId;
}
