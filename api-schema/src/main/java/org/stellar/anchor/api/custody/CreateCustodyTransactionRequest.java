package org.stellar.anchor.api.custody;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateCustodyTransactionRequest {

  private String id;
  private String memo;
  private String memoType;
  private String protocol;
  private String fromAccount;
  private String toAccount;
  private String amountIn;
  private String amountInAsset;
  private String amountOut;
  private String amountOutAsset;
  private String kind;
  private String requestAssetCode;
  private String requestAssetIssuer;
}
