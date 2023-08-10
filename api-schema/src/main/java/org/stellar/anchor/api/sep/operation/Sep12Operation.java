package org.stellar.anchor.api.sep.operation;

import java.util.Map;
import lombok.Data;

@Data
public class Sep12Operation {
  Types sender;
  Types receiver;

  @Data
  public static class Types {
    Map<String, Type> types;
  }

  @Data
  public static class Type {
    String description;
  }
}
