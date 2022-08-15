package org.stellar.anchor.sep31;

import org.stellar.anchor.api.converter.Converter;
import org.stellar.anchor.api.sep.sep31.Sep31GetTransactionResponse;
import org.stellar.anchor.api.shared.Amount;

public interface RefundPayment {
  String getId();

  void setId(String id);

  String getAmount();

  void setAmount(String amount);

  String getFee();

  void setFee(String fee);

  static Sep31RefundPaymentConverter sep31Converter(Sep31TransactionStore factory) {
    return new Sep31RefundPaymentConverter(factory);
  }

  static CallbackApiRefundPaymentConverter callbackConverter(
      Sep31TransactionStore factory, String assetName) {
    return new CallbackApiRefundPaymentConverter(factory, assetName);
  }

  class Sep31RefundPaymentConverter
      extends Converter<RefundPayment, Sep31GetTransactionResponse.Sep31RefundPayment> {
    private final Sep31TransactionStore factory;

    public Sep31RefundPaymentConverter(Sep31TransactionStore factory) {
      super(null, null);
      this.factory = factory;
    }

    /**
     * Converts a SEP-31 database Entity into a SEP-31 protocol response DTO (Data Transfer Object).
     *
     * @param refundPayment is the RefundPayment database Entity to be converted.
     * @return a Sep31GetTransactionResponse.Sep31RefundPayment DTO (Data Transfer Object).
     */
    @Override
    public Sep31GetTransactionResponse.Sep31RefundPayment convert(RefundPayment refundPayment) {
      if (refundPayment == null) {
        return null;
      }

      return Sep31GetTransactionResponse.Sep31RefundPayment.builder()
          .id(refundPayment.getId())
          .amount(refundPayment.getAmount())
          .fee(refundPayment.getFee())
          .build();
    }

    public RefundPayment inverse(
        Sep31GetTransactionResponse.Sep31RefundPayment sep31RefundPaymentResponse) {
      if (sep31RefundPaymentResponse == null) {
        return null;
      }
      RefundPayment refundPayment = factory.newRefundPayment();
      refundPayment.setAmount(sep31RefundPaymentResponse.getAmount());
      refundPayment.setFee(sep31RefundPaymentResponse.getFee());
      refundPayment.setId(sep31RefundPaymentResponse.getId());
      return refundPayment;
    }
  }

  class CallbackApiRefundPaymentConverter
      extends Converter<RefundPayment, org.stellar.anchor.api.shared.RefundPayment> {
    Sep31TransactionStore factory;
    String assetName;

    public CallbackApiRefundPaymentConverter(Sep31TransactionStore factory, String assetName) {
      super(null, null);
      this.factory = factory;
      this.assetName = assetName;
    }

    /**
     * Converts a SEP-31 database Entity into a platformApi's RefundPayment DTO (Data Transfer
     * Object).
     *
     * @param refundPayment is the RefundPayment database Entity to be converted.
     * @return a PlatformApi's RefundPayment DTO (Data Transfer Object).
     */
    @Override
    public org.stellar.anchor.api.shared.RefundPayment convert(RefundPayment refundPayment) {
      if (refundPayment == null) {
        return null;
      }

      return org.stellar.anchor.api.shared.RefundPayment.builder()
          .id(refundPayment.getId())
          .idType(org.stellar.anchor.api.shared.RefundPayment.IdType.STELLAR)
          .amount(new Amount(refundPayment.getAmount(), assetName))
          .fee(new Amount(refundPayment.getFee(), assetName))
          .requestedAt(null)
          .refundedAt(null)
          .build();
    }

    /**
     * Converts a platformApi's RefundPayment DTO (Data Transfer Object) into a RefundPayment
     * database Entity.
     *
     * @param sep31RefundPayment is the CallbackApi's RefundPayment DTO to be converted.
     * @return a RefundPayment database Entity.
     */
    @Override
    public RefundPayment inverse(org.stellar.anchor.api.shared.RefundPayment sep31RefundPayment) {
      if (sep31RefundPayment == null) {
        return null;
      }

      RefundPayment refundPayment = this.factory.newRefundPayment();
      refundPayment.setId(sep31RefundPayment.getId());
      refundPayment.setAmount(sep31RefundPayment.getAmount().getAmount());
      refundPayment.setFee(sep31RefundPayment.getFee().getAmount());
      return refundPayment;
    }
  }
}
