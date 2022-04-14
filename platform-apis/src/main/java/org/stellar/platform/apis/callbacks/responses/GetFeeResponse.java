package org.stellar.platform.apis.callbacks.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.stellar.platform.apis.shared.Amount;

@Data
@AllArgsConstructor
public class GetFeeResponse {
  Amount fee;
}
