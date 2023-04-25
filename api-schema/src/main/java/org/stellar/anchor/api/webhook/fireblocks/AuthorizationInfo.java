package org.stellar.anchor.api.webhook.fireblocks;

import java.util.List;
import lombok.Data;

@Data
public class AuthorizationInfo {
  private Boolean allowOperatorAsAuthorizer;
  private String logic;
  private List<AuthorizationGroup> groups;
}
