package org.stellar.platform.apis.platform.responses;

import lombok.Data;
import org.stellar.platform.apis.shared.Transaction;

import java.util.List;

@Data
public class PatchTransactionsResponse {
  List<Transaction> records;

  public PatchTransactionsResponse(List<Transaction> records) {
    this.records = records;
  }
}
