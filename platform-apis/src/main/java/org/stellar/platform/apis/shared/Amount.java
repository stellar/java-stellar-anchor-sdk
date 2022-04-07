package org.stellar.platform.apis.shared;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Amount {
  String amount;
  String asset;
}
