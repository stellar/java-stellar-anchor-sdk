package org.stellar.anchor.api.shared;

import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class StellarTransaction {
  String id;
  String memo;
  String memoType;
  Instant createdAt;
  String envelope;
  Long ledgerNumber;
  List<StellarPayment> payments;
}
