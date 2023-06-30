package org.stellar.anchor.platform.action.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyOnchainFundsSentRequest extends RpcParamsRequest {

  @NotBlank private String stellarTransactionId;
}