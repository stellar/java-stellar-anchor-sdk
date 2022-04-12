 package org.stellar.anchor.event.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Amount {
  String amount;
  String asset;

  public Amount(){}
}
