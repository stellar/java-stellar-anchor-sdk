package org.stellar.anchor.api.webhook.fireblocks;

import lombok.Data;

@Data
public class AmountInfo {
  private String amount;
  private String requestedAmount;
  private String netAmount;
  private String amountUSD;
}
