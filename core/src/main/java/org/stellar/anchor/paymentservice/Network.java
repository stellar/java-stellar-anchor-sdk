package org.stellar.anchor.paymentservice;

import lombok.Getter;

@Getter
public enum Network {
  CIRCLE("circle", "circle"),
  STELLAR("stellar", "stellar"),
  BANK_WIRE("bank_wire", "iso4217");

  private final String name;

  /**
   * Currency prefix is based on SEP-38 identification format:
   * https://github.com/stellar/stellar-protocol/blob/65c9c20/ecosystem/sep-0038.md#asset-identification-format
   */
  private final String currencyPrefix;

  Network(String name, String currencyPrefix) {
    this.name = name;
    this.currencyPrefix = currencyPrefix;
  }

  @Override
  public String toString() {
    return name;
  }
}
