package org.stellar.anchor.api.asset;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public interface AssetInfo {
  String NATIVE_ASSET_CODE = "native";

  // For `id` field, see anchor-asset-default-values.yaml
  String getId();

  Schema getSchema();

  String getCode();

  String getIssuer();

  Integer getSignificantDecimals();

  Sep31Info getSep31();

  Sep38Info getSep38();

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  class Field {
    String description;
    List<String> choices;
    boolean optional;
  }

  enum Schema {
    @SerializedName("stellar")
    STELLAR("stellar"),

    @SerializedName("iso4217")
    ISO_4217("iso4217");

    private final String name;

    Schema(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
