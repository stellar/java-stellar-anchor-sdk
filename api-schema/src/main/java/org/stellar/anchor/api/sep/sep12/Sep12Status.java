package org.stellar.anchor.api.sep.sep12;

import com.google.gson.annotations.SerializedName;

public enum Sep12Status {
  @SerializedName("NEEDS_INFO")
  NEEDS_INFO("NEEDS_INFO"),

  @SerializedName("ACCEPTED")
  ACCEPTED("ACCEPTED"),

  @SerializedName("PROCESSING")
  PROCESSING("PROCESSING"),

  @SerializedName("REJECTED")
  REJECTED("REJECTED");

  private final String name;

  Sep12Status(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
