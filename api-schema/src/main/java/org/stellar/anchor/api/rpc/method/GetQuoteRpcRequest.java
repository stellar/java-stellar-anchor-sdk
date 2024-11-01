package org.stellar.anchor.api.rpc.method;

import com.google.gson.annotations.SerializedName;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class GetQuoteRpcRequest extends RpcMethodParamsRequest {
  @SerializedName("quote_id")
  @NonNull
  private String quoteId;
}
