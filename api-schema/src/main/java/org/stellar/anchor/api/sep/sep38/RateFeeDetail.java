package org.stellar.anchor.api.sep.sep38;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RateFeeDetail {
  String name;
  String description;
  String amount;

  public RateFeeDetail(String name, String amount) {
    this.name = name;
    this.amount = amount;
  }
}
