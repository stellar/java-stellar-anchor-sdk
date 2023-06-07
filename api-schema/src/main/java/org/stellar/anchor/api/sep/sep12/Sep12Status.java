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
  REJECTED("REJECTED"),

  @SerializedName("VERIFICATION_REQUIRED")
  VERIFICATION_REQUIRED("VERIFICATION_REQUIRED");

  private final String name;

  Sep12Status(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public static Sep12Status fromName(String name) {
    for (Sep12Status status : Sep12Status.values()) {
      if (status.getName().equals(name)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Invalid status name: " + name);
  }
}
