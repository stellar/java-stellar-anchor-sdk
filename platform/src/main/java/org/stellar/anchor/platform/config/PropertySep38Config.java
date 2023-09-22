package org.stellar.anchor.platform.config;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.stellar.anchor.config.Sep38Config;

@Data
public class PropertySep38Config implements Sep38Config {
  boolean enabled;

  @SerializedName("requires_sep10")
  boolean requiresSep10;
}
