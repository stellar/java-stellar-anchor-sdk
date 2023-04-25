package org.stellar.anchor.api.webhook.fireblocks;

import java.util.List;
import lombok.Data;

@Data
public class AuthorizationGroup {
  private Long th;
  private List<Object> users;
}
