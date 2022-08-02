package org.stellar.anchor.platform.payment.common;

import lombok.Data;
import reactor.util.annotation.Nullable;

/**
 * Contains the configuration options needed for an external user to make a deposit into the desired
 * account. This class will be needed if the implementation allows users to make deposits using
 * intermediary networks. For instance, when a user wants to make a deposit to their Circle account
 * through a Stellar payment.
 *
 * @see DepositInstructions
 * @see PaymentService#getDepositInstructions(DepositRequirements)
 */
@Data
public class DepositRequirements {
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
   * The network where the deposit will happen. After the deposit is performed on that network it
   * will be reflected in the beneficiary user balance. Time for confirmation and reconciliation may
   * be needed depending on the network used.
   */
  PaymentNetwork intermediaryPaymentNetwork;

  @Nullable String intermediaryAccountId;

  /**
   * The name of the currency that will be ultimately credited into the beneficiary user account. It
   * should obey the {scheme}:{identifier} format described in <a
   * href="https://stellar.org/protocol/sep-38#asset-identification-format">SEP-38</a>.
   */
  String beneficiaryCurrencyName;

  /**
   * Constructor of the DepositConfiguration class.
   *
   * @param beneficiaryAccountId Identifier of the account who will receive the payment.
   * @param intermediaryPaymentNetwork The network where the deposit will be made. After the deposit
   *     is performed on that network it will be reflected in the beneficiary user balance
   * @param beneficiaryCurrencyName The name of the currency that will be ultimately credited into
   *     the beneficiary user account. For instance, if you want the instructions to receive USD
   *     credits in a Circle account using Stellar as an intermediary medium, the currency name
   *     should be "circle:USD".
   */
  public DepositRequirements(
      String beneficiaryAccountId,
      PaymentNetwork intermediaryPaymentNetwork,
      String beneficiaryCurrencyName) {
    this(beneficiaryAccountId, null, intermediaryPaymentNetwork, null, beneficiaryCurrencyName);
  }

  /**
   * Constructor of the DepositConfiguration class.
   *
   * @param beneficiaryAccountId Identifier of the account who will receive the payment.
   * @param beneficiaryAccountIdTag Complementary identifier of the account who will receive the
   *     payment. May be mandatory depending on the implementation.
   * @param intermediaryPaymentNetwork The network where the deposit will be made. After the deposit
   *     is performed on that network it will be reflected in the beneficiary user balance
   * @param beneficiaryCurrencyName The name of the currency that will be ultimately credited into
   *     the beneficiary user account. For instance, if you want the instructions to receive USD
   *     credits in a Circle account using Stellar as an intermediary medium, the currency name
   *     should be "circle:USD".
   */
  public DepositRequirements(
      String beneficiaryAccountId,
      @Nullable String beneficiaryAccountIdTag,
      PaymentNetwork intermediaryPaymentNetwork,
      String beneficiaryCurrencyName) {
    this(
        beneficiaryAccountId,
        beneficiaryAccountIdTag,
        intermediaryPaymentNetwork,
        null,
        beneficiaryCurrencyName);
  }

  /**
   * Constructor of the DepositConfiguration class.
   *
   * @param beneficiaryAccountId Identifier of the account who will receive the payment.
   * @param beneficiaryAccountIdTag Complementary identifier of the account who will receive the
   *     payment. May be mandatory depending on the implementation.
   * @param intermediaryPaymentNetwork The network where the deposit will be made. After the deposit
   *     is performed on that network it will be reflected in the beneficiary user balance
   * @param intermediaryAccountId The id of the intermediary account. It may be mandatory for some
   *     cases.
   * @param beneficiaryCurrencyName The name of the currency that will be ultimately credited into
   *     the beneficiary user account. For instance, if you want the instructions to receive USD
   *     credits in a Circle account using Stellar as an intermediary medium, the currency name
   *     should be "circle:USD".
   */
  public DepositRequirements(
      String beneficiaryAccountId,
      @Nullable String beneficiaryAccountIdTag,
      PaymentNetwork intermediaryPaymentNetwork,
      @Nullable String intermediaryAccountId,
      String beneficiaryCurrencyName) {
    this.beneficiaryAccountId = beneficiaryAccountId;
    this.beneficiaryAccountIdTag = beneficiaryAccountIdTag;
    this.intermediaryPaymentNetwork = intermediaryPaymentNetwork;
    this.intermediaryAccountId = intermediaryAccountId;
    this.beneficiaryCurrencyName = beneficiaryCurrencyName;
  }
}
