package org.stellar.anchor.api.webhook.fireblocks;

import lombok.Data;

@Data
public class FireblocksEventObject {
  private String type;
  private String tenantId;
  private Long timestamp;
  private TransactionDetails data;
}
