package org.stellar.anchor.api.sep.sep24;

import lombok.Data;

@Data
public class Sep24GetTransactionResponse {
  TransactionResponse transaction;

  public static Sep24GetTransactionResponse of(TransactionResponse tr) {
    Sep24GetTransactionResponse gtr = new Sep24GetTransactionResponse();
    gtr.transaction = tr;
    return gtr;
  }
}
