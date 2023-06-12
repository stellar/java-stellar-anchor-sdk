package org.stellar.anchor.api.shared;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvidedCustomerField {
  String type;
  String description;
  List<String> choices;
  Boolean optional;
  String status;
  String error;
}
