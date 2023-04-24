package org.stellar.anchor.api.webhook.fireblocks;

import lombok.Data;

@Data
public class SystemMessageInfo {
  private SystemMessageInfoType type;
  private String message;
}
