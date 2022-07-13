package org.stellar.anchor.util;

import lombok.Getter;
import lombok.Setter;

public class JsonLog {
  @Getter @Setter Integer time;

  @Getter @Setter String source;

  @Getter @Setter String index;

  @Getter @Setter EventObjectJson event;
}
