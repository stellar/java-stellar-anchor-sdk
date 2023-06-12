package org.stellar.anchor.api.sep.sep12;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * The response body of the GET /customer endpoint of SEP-12.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0012.md#fields">Refer
 *     to SEP-12</a>
 */
@Data
@Builder
public class Sep12GetCustomerResponse {
  String id;
  Sep12Status status;

  Map<String, Field> fields;

  @SerializedName("provided_fields")
  Map<String, ProvidedField> providedFields;

  String message;
}
