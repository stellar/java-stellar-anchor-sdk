package org.stellar.anchor.event.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class StellarId {
  String id;
  String account;

  public StellarId() {}
}
