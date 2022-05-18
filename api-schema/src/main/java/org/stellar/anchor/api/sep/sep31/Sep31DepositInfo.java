package org.stellar.anchor.api.sep.sep31;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Sep31DepositInfo {
  private final String stellarAddress;
  private final String memo;
  private final String memoType;
}
