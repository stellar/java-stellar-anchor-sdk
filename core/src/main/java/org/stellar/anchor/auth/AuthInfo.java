package org.stellar.anchor.auth;

import lombok.Data;

@Data
public class AuthInfo {
  AuthType type;
  String secret;
  String expirationMilliseconds;
}
