package org.stellar.anchor.platform.dto.fireblocks;

import lombok.Data;

@Data
public class CreateNewDepositAddressRequestDto {

  private String description;
  private String customerRefId;
}
