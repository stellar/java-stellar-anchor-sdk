package org.stellar.anchor.api.webhook.fireblocks;

import lombok.Data;

@Data
public class RewardsInfo {
  private String srcRewards;
  private String destRewards;
}
