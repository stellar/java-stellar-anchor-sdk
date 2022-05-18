package org.stellar.anchor.api.callback;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.stellar.anchor.api.shared.Amount;

@Data
@AllArgsConstructor
public class GetFeeResponse {
  Amount fee;
}
