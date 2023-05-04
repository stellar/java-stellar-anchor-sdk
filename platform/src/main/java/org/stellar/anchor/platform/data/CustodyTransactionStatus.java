package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;

public enum CustodyTransactionStatus {
  @SerializedName("created")
  CREATED("created"),
  @SerializedName("sent")
  SUBMITTED("submitted"),
  @SerializedName("received")
  RECEIVED("received");

  private final String status;

  CustodyTransactionStatus(String status) {
    this.status = status;
  }

  public String toString() {
    return status;
  }
}
