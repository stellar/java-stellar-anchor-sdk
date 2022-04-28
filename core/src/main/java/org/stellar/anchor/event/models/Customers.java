package org.stellar.anchor.event.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Customers {
  StellarId sender;
  StellarId receiver;

  public Customers() {}
}
