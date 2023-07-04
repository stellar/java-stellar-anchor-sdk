package org.stellar.anchor.api.rpc.action;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AmountRequest {

  @NotBlank private String amount;

  @NotBlank private String asset;
}
