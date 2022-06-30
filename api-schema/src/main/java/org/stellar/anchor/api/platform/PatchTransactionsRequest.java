package org.stellar.anchor.api.platform;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PatchTransactionsRequest {
  List<PatchTransactionRequest> records;
}
