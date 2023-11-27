package org.stellar.anchor.api.sep.sep38;

import com.google.gson.annotations.SerializedName;

public enum Sep38Context {
  @SerializedName("sep6")
  SEP6("sep6"),

  @SerializedName("sep24")
  SEP24("sep24"),

  @SerializedName("sep31")
  SEP31("sep31");

  private final String name;

  Sep38Context(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
