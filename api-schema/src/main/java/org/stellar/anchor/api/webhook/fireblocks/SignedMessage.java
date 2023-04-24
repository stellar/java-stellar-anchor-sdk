package org.stellar.anchor.api.webhook.fireblocks;

import lombok.Data;

@Data
public class SignedMessage {
  private String content;
  private String algorithm;
  private Long[] derivationPath;
  private Object signature;
  private String publicKey;
}
