package org.stellar.anchor.api.sep.sep38;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
// Used in SEP-38 to detail costs (like fees and taxes) embedded in the price.
public class PriceDetail {
  String name;
  String asset;
  String value;

  public PriceDetail() {}
}
