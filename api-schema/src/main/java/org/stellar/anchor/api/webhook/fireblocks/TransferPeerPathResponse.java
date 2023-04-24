package org.stellar.anchor.api.webhook.fireblocks;

import lombok.Data;

@Data
public class TransferPeerPathResponse {
  private TransferPeerPathResponseType type;
  private String id;
  private String name;
  private TransferPeerPathResponseSubType subType;
}
