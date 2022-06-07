package org.stellar.anchor.api.sep.sep31;

import lombok.Builder;
import lombok.Data;
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest.Sep31TxnFields;

@Data
@Builder
public class Sep31PatchTransactionRequest {
  String id;
  Sep31TxnFields fields;
}
