package org.stellar.anchor.platform.dto.fireblocks;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateNewDepositAddressResponseDto {

  private String address;
  private String legacyAddress;
  private String enterpriseAddress;
  private String tag;
  private Integer bip44AddressIndex;
}
