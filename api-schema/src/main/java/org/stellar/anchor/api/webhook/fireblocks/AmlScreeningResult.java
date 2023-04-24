package org.stellar.anchor.api.webhook.fireblocks;

import lombok.Data;

@Data
public class AmlScreeningResult {
  private String provider;
  private String payload;
}
