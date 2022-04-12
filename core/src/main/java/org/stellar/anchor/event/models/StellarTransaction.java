package org.stellar.anchor.event.models;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class StellarTransaction {
  String id;
  String memo;

  @SerializedName("memo_type")
  String memoType;

  @SerializedName("created_at")
  Instant createdAt;

  String envelope;
  Payment[] payments;

  public StellarTransaction(){}
}

@Data
@Builder
@AllArgsConstructor
class Customers {
  StellarId sender;
  StellarId receiver;

  public Customers(){}
}
