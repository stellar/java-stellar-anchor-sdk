package org.stellar.anchor.auth;

import com.google.gson.annotations.SerializedName;

public enum AuthType {
  @SerializedName("none")
  NONE,
  @SerializedName("api_key")
  API_KEY,
  @SerializedName("jwt")
  JWT
}
