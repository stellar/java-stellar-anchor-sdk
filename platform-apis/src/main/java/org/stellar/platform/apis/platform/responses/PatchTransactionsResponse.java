package org.stellar.platform.apis.platform.responses;

import java.util.List;
import lombok.Data;
import org.stellar.platform.apis.shared.Transaction;

@Data
public class PatchTransactionsResponse {
  List<Transaction> records;

  public PatchTransactionsResponse(List<Transaction> records) {
    this.records = records;
  }
}
