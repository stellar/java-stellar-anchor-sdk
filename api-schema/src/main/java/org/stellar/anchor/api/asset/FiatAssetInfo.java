package org.stellar.anchor.api.asset;

import static org.stellar.anchor.api.asset.AssetInfo.Schema.*;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class FiatAssetInfo implements AssetInfo {
  String id;

  @SerializedName("significant_decimals")
  Integer significantDecimals;

  Sep31Info sep31;
  Sep38Info sep38;

  @Override
  public Schema getSchema() {
    return ISO_4217;
  }

  @Override
  public String getCode() {
    return getId().split(":")[1];
  }

  @Override
  public String getIssuer() {
    return null;
  }
}
