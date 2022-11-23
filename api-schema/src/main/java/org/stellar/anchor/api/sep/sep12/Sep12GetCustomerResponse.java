package org.stellar.anchor.api.sep.sep12;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;

/**
 * Refer to SEP-12.
 *
 * <p><a
 * href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0012.md#response">...</a>
 */
@Data
public class Sep12GetCustomerResponse {
  String id;
  Sep12Status status;

  Map<String, Field> fields;

  @SerializedName("provided_fields")
  Map<String, ProvidedField> providedFields;

  String message;
}
