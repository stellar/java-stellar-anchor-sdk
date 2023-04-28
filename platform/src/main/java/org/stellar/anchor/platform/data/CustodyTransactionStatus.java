package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

public enum CustodyTransactionStatus {
  @SerializedName("created")
  CREATED("created"),
  @SerializedName("sent")
  SENT("sent"),
  @SerializedName("received")
  RECEIVED("received");

  private final String status;

  CustodyTransactionStatus(String status) {
    this.status = status;
  }

  public static CustodyTransactionStatus from(String status) {

    for (CustodyTransactionStatus sts : values()) {
      if (Objects.equals(sts.status, status)) {
        return sts;
      }
    }
    throw new IllegalArgumentException("No matching constant for [" + status + "]");
  }

  public String toString() {
    return status;
  }

  public String getName() {
    return status;
  }

  public String getStatus() {
    return status;
  }
}
