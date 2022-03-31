package org.stellar.anchor.dto.sep31;

import lombok.Builder;
import lombok.Data;
import org.stellar.anchor.asset.AssetInfo;

@Data
@Builder
public class Sep31PatchTransactionRequest {
  String id;
  AssetInfo.Sep31TxnFields fields;
}
