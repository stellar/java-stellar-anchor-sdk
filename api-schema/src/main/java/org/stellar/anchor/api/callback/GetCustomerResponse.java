package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.api.shared.CustomerField;
import org.stellar.anchor.api.shared.ProvidedCustomerField;

/**
 * The response body of the GET /customer endpoint.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/ap/Callbacks%20API.yml">Callback
 *     API</a>
 */
@Data
public class GetCustomerResponse {
  String id;
  String status;
  Map<String, CustomerField> fields;

  @SerializedName("provided_fields")
  Map<String, ProvidedCustomerField> providedFields;

  String message;
}
