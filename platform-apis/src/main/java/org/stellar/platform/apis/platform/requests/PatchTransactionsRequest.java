package org.stellar.platform.apis.platform.requests;

import java.util.List;
import lombok.Data;

@Data
public class PatchTransactionsRequest {
  List<PatchTransactionRequest> records;
}
