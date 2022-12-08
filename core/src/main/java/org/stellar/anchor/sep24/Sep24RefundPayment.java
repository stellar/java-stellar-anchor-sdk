package org.stellar.anchor.sep24;

@SuppressWarnings("unused")
public interface Sep24RefundPayment {
  /**
   * The payment ID that can be used to identify the refund payment. This is either a Stellar
   * transaction hash or an off-chain payment identifier, such as a reference number provided to the
   * user when the refund was initiated. This id is not guaranteed to be unique.
   *
   * @return the ID field.
   */
  String getId();

  void setId(String id);
  /**
   * The amount sent back to the user for the payment identified by id, in units of amount_in_asset.
   *
   * @return the amount.
   */
  String getAmount();

  void setAmount(String amount);
  /**
   * The amount charged as a fee for processing the refund, in units of amount_in_asset.
   *
   * @return the fee.
   */
  String getFee();

  void setFee(String fee);
}
