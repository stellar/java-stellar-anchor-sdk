package org.stellar.anchor.api.shared;

import java.util.List;
import lombok.Data;

@Data
public class ProvidedField {
  String type;
  String description;
  List<String> choices;
  Boolean optional;
  String status;
  String error;
}
