package org.stellar.anchor.api.sep.operation;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.api.sep.AssetInfo;

@Data
public class Sep31Operation {
  @SerializedName("quotes_supported")
  boolean quotesSupported;

  @SerializedName("quotes_required")
  boolean quotesRequired;

  Sep12Operation sep12;
  Fields fields;

  @Data
  public static class Fields {
    Map<String, AssetInfo.Field> transaction;
  }
}
