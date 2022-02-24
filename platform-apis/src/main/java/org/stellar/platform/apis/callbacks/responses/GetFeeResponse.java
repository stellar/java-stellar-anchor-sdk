package org.stellar.platform.apis.callbacks.responses;

import lombok.Data;
import org.stellar.platform.apis.shared.Amount;

@Data
public class GetFeeResponse {
  Amount fee;
}
