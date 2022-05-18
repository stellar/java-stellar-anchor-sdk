package org.stellar.anchor.event.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Customers {
  StellarId sender;
  StellarId receiver;

  public Customers() {}
}
