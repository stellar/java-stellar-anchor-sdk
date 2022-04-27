package org.stellar.anchor.api.platform;

import java.util.List;
import lombok.Data;
import org.stellar.anchor.api.shared.Transaction;

@Data
public class PatchTransactionsResponse {
  List<Transaction> records;

  public PatchTransactionsResponse(List<Transaction> records) {
    this.records = records;
  }
}
