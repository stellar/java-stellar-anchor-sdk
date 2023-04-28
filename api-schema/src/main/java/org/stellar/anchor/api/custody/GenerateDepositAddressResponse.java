package org.stellar.anchor.api.custody;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerateDepositAddressResponse {

  private String address;
  private String memo;
  private String memoType;
}
