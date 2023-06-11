package org.stellar.anchor.api.callback;

import com.google.gson.Gson;
import lombok.Data;
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerResponse;

/**
 * The response body of the PUT /customer endpoint.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/ap/Callbacks%20API.yml">Callback
 *     API</a>
 */
@Data
public class PutCustomerResponse {
  String id;

  public static Sep12PutCustomerResponse to(PutCustomerResponse response) {
    Gson gson = new Gson();
    return new Gson().fromJson(gson.toJson(response), Sep12PutCustomerResponse.class);
  }
}
