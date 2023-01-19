package org.stellar.anchor.api.platform;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PatchTransactionRequest {
  PlatformTransactionData transaction;
}
