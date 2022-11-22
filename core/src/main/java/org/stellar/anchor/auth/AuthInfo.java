package org.stellar.anchor.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthInfo {
  AuthType type;
  String secret;
  String expirationMilliseconds;
}
