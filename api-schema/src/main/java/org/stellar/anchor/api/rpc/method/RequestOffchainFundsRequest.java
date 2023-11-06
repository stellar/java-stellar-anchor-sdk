package org.stellar.anchor.api.rpc.method;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.stellar.anchor.api.shared.InstructionField;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RequestOffchainFundsRequest extends RpcMethodParamsRequest {

  @SerializedName("amount_in")
  private AmountAssetRequest amountIn;

  @SerializedName("amount_out")
  private AmountAssetRequest amountOut;

  @SerializedName("amount_fee")
  private AmountAssetRequest amountFee;

  @SerializedName("amount_expected")
  private AmountRequest amountExpected;

  @SerializedName("instructions")
  Map<String, InstructionField> instructions;
}
