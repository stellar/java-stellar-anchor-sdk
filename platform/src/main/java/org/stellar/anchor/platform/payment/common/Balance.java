package org.stellar.anchor.platform.payment.common;

import lombok.Data;

@Data
public class Balance {
  String amount;
  /**
   * The name of the currency that will be ultimately credited into the beneficiary user account. It
   * should obey the {scheme}:{identifier} format described in <a
   * href="https://stellar.org/protocol/sep-38#asset-identification-format">SEP-38</a>.
   */
  String currencyName;

  /**
   * A balance object representing amount and currency name.
   *
   * @param amount The amount of assets in the balance or payment.
   * @param currencyName The name of the currency that will be ultimately credited into the
   *     destination account. It should obey the {scheme}:{identifier} format described in <a
   *     href="https://stellar.org/protocol/sep-38#asset-identification-format">SEP-38</a>.
   */
  public Balance(String amount, String currencyName) {
    this.amount = amount;
    this.currencyName = currencyName;
  }
}
