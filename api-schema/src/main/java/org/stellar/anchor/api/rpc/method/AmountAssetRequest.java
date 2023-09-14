package org.stellar.anchor.api.rpc.method;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AmountAssetRequest extends AmountRequest {

  @NotBlank private String asset;

  public AmountAssetRequest(String amount, String asset) {
    super(amount);
    this.asset = asset;
  }
}
