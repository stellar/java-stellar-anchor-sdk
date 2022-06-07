package org.stellar.anchor.api.shared;

import java.util.List;
import lombok.Data;

@Data
public class ProvidedCustomerField {
  String type;
  String description;
  List<String> choices;
  Boolean optional;
  String status;
  String error;
}
