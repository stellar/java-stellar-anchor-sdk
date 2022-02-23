package org.stellar.anchor.dto.sep12;

import java.util.List;
import lombok.Data;

/**
 * Refer to SEP-12.
 *
 * <p>https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0012.md#provided-fields
 */
@Data
public class ProvidedField {
  Field.Type type;
  String description;
  List<String> choices;
  Boolean optional;
  Sep12Status status;
  String error;
}
