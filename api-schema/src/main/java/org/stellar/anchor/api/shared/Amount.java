package org.stellar.anchor.api.shared;

import javax.annotation.Nullable;
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

  public static @Nullable Amount create(String amount, String asset) {
    if (amount == null && asset == null) {
      return null;
    }

    return new Amount(amount, asset);
  }
}
