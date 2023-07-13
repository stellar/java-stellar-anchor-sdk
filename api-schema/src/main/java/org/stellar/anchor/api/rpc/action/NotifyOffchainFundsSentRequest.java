package org.stellar.anchor.api.rpc.action;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NotifyOffchainFundsSentRequest extends RpcActionParamsRequest {

  private Instant fundsSentAt;
  private String externalTransactionId;
}
