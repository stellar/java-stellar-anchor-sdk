package org.stellar.anchor.api.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Amount {
  String amount;
  String asset;

  public Amount() {}
}
