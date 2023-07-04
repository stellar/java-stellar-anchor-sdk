package org.stellar.anchor.api.asset.operation;

import java.util.Map;
import lombok.Data;

@Data
public class Sep12Operation {
  private Types sender;
  private Types receiver;

  @Data
  public static class Types {
    private Map<String, Type> types;
  }

  @Data
  public static class Type {
    private String description;
  }
}
