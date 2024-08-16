package org.stellar.anchor.api.asset;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.stellar.anchor.api.sep.AssetInfo;

@Data
public class StellarAssetInfo extends AssetInfo {

  String issuer;

  @SerializedName("distribution_account")
  String distributionAccount;
}
