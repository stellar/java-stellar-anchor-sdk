package org.stellar.anchor.api.sep.sep31;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.api.sep.AssetInfo;

/**
 * The response body of the /info endpoint of the SEP-31.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0031.md#fields">Refer
 *     to SEP-31</a>
 */
@Data
public class Sep31InfoResponse {
  Map<String, AssetResponse> receive;

  @Data
  public static class AssetResponse {
    Boolean enabled = true;

    @SerializedName("quotes_supported")
    Boolean quotesSupported;

    @SerializedName("quotes_required")
    Boolean quotesRequired;

    @SerializedName("fee_fixed")
    Integer feeFixed;

    @SerializedName("fee_percent")
    Integer feePercent;

    @SerializedName("min_amount")
    Long minAmount;

    @SerializedName("max_amount")
    Long maxAmount;

    AssetInfo.Sep12Operation sep12;
    AssetInfo.Sep31TxnFieldSpecs fields;
  }
}
