package org.stellar.anchor.api.platform;

import java.util.List;
import lombok.Data;

@Data
public class PatchTransactionsResponse {
  List<GetTransactionResponse> records;

  public PatchTransactionsResponse(List<GetTransactionResponse> records) {
    this.records = records;
  }
}
