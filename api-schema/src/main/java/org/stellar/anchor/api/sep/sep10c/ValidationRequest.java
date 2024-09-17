package org.stellar.anchor.api.sep.sep10c;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Builder
@Data
public class ValidationRequest {
  String invocation;
  String serverCredentials;
  String credentials;
}
