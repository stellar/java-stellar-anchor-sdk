package org.stellar.anchor.api.asset;

import lombok.Data;

@Data
public class DepositWithdrawInfo {
  Boolean enabled = false;
  DepositWithdrawOperation deposit;
  DepositWithdrawOperation withdraw;
}
