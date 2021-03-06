package org.stellar.anchor.api.platform;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PatchTransactionsRequest {
  List<PatchTransactionRequest> records;
}
