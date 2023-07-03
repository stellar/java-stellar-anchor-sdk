package org.stellar.anchor.api.rpc.action;

import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class AmountRequest {

  @NotBlank private String amount;

  @NotBlank private String asset;
}
