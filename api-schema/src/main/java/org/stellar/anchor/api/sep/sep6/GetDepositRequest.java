package org.stellar.anchor.api.sep.sep6;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Builder
@Data
public class GetDepositRequest {
  @NonNull
  @SerializedName("asset_code")
  private String assetCode;

  @NonNull String account;

  @SerializedName("memo_type")
  String memoType;

  String memo;

  @SerializedName("email_address")
  String emailAddress;

  @NonNull String type;

  @SerializedName("wallet_name")
  String walletName;

  @SerializedName("wallet_url")
  String walletUrl;

  String lang;

  @NonNull String amount;

  @SerializedName("country_code")
  String countryCode;

  @SerializedName("claimable_balances_supported")
  Boolean claimableBalancesSupported;
}
