package org.stellar.anchor.platform.dto.fireblocks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CreateNewDepositAddressRequestDto {

  private String description;
  private String customerRefId;
}
