package org.stellar.anchor.api.webhook.fireblocks;

import lombok.Data;

@Data
public class SystemMessageInfo {
  private String type;
  private String message;
}
