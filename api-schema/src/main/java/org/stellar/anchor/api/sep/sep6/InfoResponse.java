package org.stellar.anchor.api.sep.sep6;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.stellar.anchor.api.sep.AssetInfo;

@Data
@Builder
public class InfoResponse {

  Map<String, DepositAssetResponse> deposit;

  @SerializedName("deposit-exchange")
  Map<String, DepositAssetResponse> depositExchange;

  Map<String, WithdrawAssetResponse> withdraw;

  @SerializedName("withdraw-exchange")
  Map<String, WithdrawAssetResponse> withdrawExchange;

  @Data
  @Builder
  public static class DepositAssetResponse {
    Boolean enabled;

    @SerializedName("authentication_required")
    Boolean authenticationRequired;

    Map<String, AssetInfo.Field> fields;
  }

  @Data
  @Builder
  public static class WithdrawAssetResponse {
    Boolean enabled;

    @SerializedName("authentication_required")
    Boolean authenticationRequired;

    Map<String, Map<String, AssetInfo.Field>> types;
  }
}
