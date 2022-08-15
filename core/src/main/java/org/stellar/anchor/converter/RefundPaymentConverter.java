package org.stellar.anchor.converter;

import org.stellar.anchor.api.sep.sep31.Sep31GetTransactionResponse;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.sep31.RefundPayment;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class RefundPaymentConverter {
  /**
   * Converts a platformApi's RefundPayment DTO (Data Transfer Object) into a RefundPayment database
   * Entity.
   *
   * @param factory is used to create a new instance of the RefundPayment database Entity.
   * @param platformApiRefundPaymentDto is the PlatformApi's RefundPayment DTO to be converted.
   * @return a RefundPayment database Entity.
   */
  public static RefundPayment toDatabaseEntity(
      Sep31TransactionStore factory,
      org.stellar.anchor.api.shared.RefundPayment platformApiRefundPaymentDto) {
    if (platformApiRefundPaymentDto == null) {
      return null;
    }

    RefundPayment refundPayment = factory.newRefundPayment();
    refundPayment.setId(platformApiRefundPaymentDto.getId());
    refundPayment.setAmount(platformApiRefundPaymentDto.getAmount().getAmount());
    refundPayment.setFee(platformApiRefundPaymentDto.getFee().getAmount());
    return refundPayment;
  }

  /**
   * Converts a SEP-31 database Entity into a SEP-31 protocol response DTO (Data Transfer Object).
   *
   * @param sep31RefundPaymentEntity is the RefundPayment database Entity to be converted.
   * @return a Sep31GetTransactionResponse.Sep31RefundPayment DTO (Data Transfer Object).
   */
  public static Sep31GetTransactionResponse.Sep31RefundPayment toSepProtocolDto(
      RefundPayment sep31RefundPaymentEntity) {
    if (sep31RefundPaymentEntity == null) {
      return null;
    }

    return Sep31GetTransactionResponse.Sep31RefundPayment.builder()
        .id(sep31RefundPaymentEntity.getId())
        .amount(sep31RefundPaymentEntity.getAmount())
        .fee(sep31RefundPaymentEntity.getFee())
        .build();
  }

  /**
   * Converts a SEP-31 database Entity into a platformApi's RefundPayment DTO (Data Transfer
   * Object).
   *
   * @param sep31RefundPaymentEntity is the RefundPayment database Entity to be converted.
   * @param assetName is the full asset name in the <a
   *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0038.md#asset-identification-format">Asset
   *     Identification Format</a>
   * @return a PlatformApi's RefundPayment DTO (Data Transfer Object).
   */
  public static org.stellar.anchor.api.shared.RefundPayment toPlatformApiDto(
      RefundPayment sep31RefundPaymentEntity, String assetName) {
    if (sep31RefundPaymentEntity == null) {
      return null;
    }

    return org.stellar.anchor.api.shared.RefundPayment.builder()
        .id(sep31RefundPaymentEntity.getId())
        .idType(org.stellar.anchor.api.shared.RefundPayment.IdType.STELLAR)
        .amount(new Amount(sep31RefundPaymentEntity.getAmount(), assetName))
        .fee(new Amount(sep31RefundPaymentEntity.getFee(), assetName))
        .requestedAt(null)
        .refundedAt(null)
        .build();
  }
}
