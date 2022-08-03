package org.stellar.anchor.api.shared;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Customers {
  StellarId sender;
  StellarId receiver;

  public Customers() {}
}
