package org.stellar.anchor.client.config;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.stellar.anchor.config.Sep38Config;

@Data
public class PropertySep38Config implements Sep38Config {
  boolean enabled;

  @SerializedName("sep10_enforced")
  boolean sep10Enforced;
}
