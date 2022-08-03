package org.stellar.anchor.platform.payment.common;

import java.util.Map;
import lombok.Data;
import reactor.util.annotation.Nullable;

/**
 * The instructions needed to make a deposit into a beneficiary account using an intermediary
 * account.
 *
 * @see DepositRequirements
 * @see PaymentService#getDepositInstructions(DepositRequirements)
 */
@Data
public class DepositInstructions {
  /**
   * The internal identifier of the beneficiary account, i.e. the account who will receive the
   * payment.
   */
  String beneficiaryAccountId;

  /**
   * A complementary identifier of the beneficiary account who will receive the payment. It might be
   * considered mandatory depending on the use case.
   */
  @Nullable String beneficiaryAccountIdTag;

  /**
   * The network of the deposit beneficiary. It is the network where the deposit will be ultimately
   * credited.
   */
  PaymentNetwork beneficiaryPaymentNetwork;

  /** The identifier of the intermediary account who will receive the deposit. */
  String intermediaryAccountId;

  /**
   * A complementary identifier of the intermediary account who will receive the deposit. It might
   * be considered mandatory depending on the use case.
   */
  @Nullable String intermediaryAccountIdTag;

  /**
   * The network where the deposit will be made. Some time after the deposit is performed on that
   * network it will be reflected in the beneficiary user balance. Time for confirmation and
   * reconciliation may vary depending on the network used.
   */
  PaymentNetwork intermediaryPaymentNetwork;

  /**
   * The name of the currency to be deposited into the intermediary network. It should obey the
   * {scheme}:{identifier} format described in <a
   * href="https://stellar.org/protocol/sep-38#asset-identification-format">SEP-38</a>.
   */
  String intermediaryCurrencyName;

  /** Extra information needed to perform the deposit. */
  @Nullable Map<String, ?> extra;

  /**
   * Constructor for the DepositInstructions class
   *
   * @param beneficiaryAccountId Identifier of the account who will receive the payment.
   * @param beneficiaryAccountIdTag Complementary identifier of the account who will receive the
   *     payment. May be mandatory depending on the implementation.
   * @param beneficiaryPaymentNetwork A complementary identifier of the intermediary account who
   *     will receive the deposit. It might be considered mandatory depending on the use case.
   * @param intermediaryAccountId The identifier of the intermediary account who will receive the
   *     deposit.
   * @param intermediaryAccountIdTag A complementary identifier of the beneficiary account who will
   *     receive the payment.
   * @param intermediaryPaymentNetwork The network where the deposit will be made. After the deposit
   *     is performed on that network it will be reflected in the beneficiary user balance
   * @param intermediaryCurrencyName The name of the currency to be deposited into the intermediary
   *     network.
   * @param extra Extra information needed to perform the deposit.
   */
  public DepositInstructions(
      String beneficiaryAccountId,
      @Nullable String beneficiaryAccountIdTag,
      PaymentNetwork beneficiaryPaymentNetwork,
      String intermediaryAccountId,
      @Nullable String intermediaryAccountIdTag,
      PaymentNetwork intermediaryPaymentNetwork,
      String intermediaryCurrencyName,
      @Nullable Map<String, ?> extra) {
    this.beneficiaryAccountId = beneficiaryAccountId;
    this.beneficiaryAccountIdTag = beneficiaryAccountIdTag;
    this.beneficiaryPaymentNetwork = beneficiaryPaymentNetwork;
    this.intermediaryAccountId = intermediaryAccountId;
    this.intermediaryAccountIdTag = intermediaryAccountIdTag;
    this.intermediaryPaymentNetwork = intermediaryPaymentNetwork;
    this.intermediaryCurrencyName = intermediaryCurrencyName;
    this.extra = extra;
  }

  /**
   * Constructor for the DepositInstructions class
   *
   * @param beneficiaryAccountId Identifier of the account who will receive the payment.
   * @param beneficiaryPaymentNetwork A complementary identifier of the intermediary account who
   *     will receive the deposit. It might be considered mandatory depending on the use case.
   * @param intermediaryAccountId The identifier of the intermediary account who will receive the
   *     deposit.
   * @param intermediaryPaymentNetwork The network where the deposit will be made. After the deposit
   *     is performed on that network it will be reflected in the beneficiary user balance
   * @param intermediaryCurrencyName The name of the currency to be deposited into the intermediary
   *     network.
   */
  public DepositInstructions(
      String beneficiaryAccountId,
      PaymentNetwork beneficiaryPaymentNetwork,
      String intermediaryAccountId,
      PaymentNetwork intermediaryPaymentNetwork,
      String intermediaryCurrencyName) {
    this(
        beneficiaryAccountId,
        null,
        beneficiaryPaymentNetwork,
        intermediaryAccountId,
        null,
        intermediaryPaymentNetwork,
        intermediaryCurrencyName,
        null);
  }
}
