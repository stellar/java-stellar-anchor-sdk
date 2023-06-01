package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * The request body of GET /customer endpoint.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/ap/Callbacks%20API.yml">Callback
 *     API</a>
 */
@Data
@Builder
public class GetCustomerRequest {
  String id;
  String account;
  String memo;

  @SerializedName("memo_type")
  String memoType;

  String type;
  String lang;
}
