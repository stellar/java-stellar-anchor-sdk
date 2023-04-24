package org.stellar.anchor.api.webhook.fireblocks;

import lombok.Data;

@Data
public class BlockInfo {
  private String blockHeight;
  private String blockHash;
}
