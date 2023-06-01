package org.stellar.anchor.api.sep.sep12;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * The request body of the DELETE /customer endpoint of SEP-12.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0012.md#fields">Refer
 *     to SEP-12</a>
 */
@Data
@Builder
public class Sep12DeleteCustomerRequest {
  String account;
  String memo;

  @SerializedName("memo_type")
  String memoType;
}
