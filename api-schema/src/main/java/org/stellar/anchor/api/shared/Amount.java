package org.stellar.anchor.api.shared;

import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@AllArgsConstructor
public class Amount {
  String amount;
  String asset;

  public Amount() {}

  public static @Nullable Amount create(String amount, String asset) {
    if (amount == null && StringUtils.isEmpty(asset)) {
      return null;
    }

    return new Amount(amount, asset);
  }
}
