package org.stellar.anchor.api.sep.operation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Field {
  String description;
  List<String> choices;
  boolean optional;
}
