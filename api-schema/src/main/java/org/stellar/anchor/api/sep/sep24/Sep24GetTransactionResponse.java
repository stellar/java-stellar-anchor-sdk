package org.stellar.anchor.api.sep.sep24;

import lombok.Data;

/** The response to the GET /transaction endpoint of SEP-24. */
@Data
public class Sep24GetTransactionResponse {
  TransactionResponse transaction;

  public static Sep24GetTransactionResponse of(TransactionResponse tr) {
    Sep24GetTransactionResponse gtr = new Sep24GetTransactionResponse();
    gtr.transaction = tr;
    return gtr;
  }
}
