package org.stellar.anchor.config;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import org.stellar.anchor.client.TempClient;

public interface ClientsConfig {

  ClientsConfigType getType();

  String getValue();

  List<TempClient> getItems();

  enum ClientsConfigType {
    @SerializedName("file")
    FILE,
    @SerializedName("inline")
    INLINE,
  }
}
