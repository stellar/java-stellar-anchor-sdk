package org.stellar.anchor.api.webhook.fireblocks;

import lombok.Data;

@Data
public class FeeInfo {
  private String networkFee;
  private String serviceFee;
}
