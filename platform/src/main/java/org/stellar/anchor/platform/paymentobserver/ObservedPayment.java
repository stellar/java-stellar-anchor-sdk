package org.stellar.anchor.platform.paymentobserver;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.apache.tomcat.util.codec.binary.Base64;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.MemoHelper;
import org.stellar.sdk.AssetTypeCreditAlphaNum;
import org.stellar.sdk.MemoHash;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentBaseOperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;
import org.stellar.sdk.xdr.MemoType;

@Builder
@Data
public class ObservedPayment {
  String id;
  Type type;

  String from;
  String to;

  String amount;
  String assetType;
  String assetCode;
  String assetIssuer;
  String assetName;

  String sourceAmount;
  String sourceAssetType;
  String sourceAssetCode;
  String sourceAssetIssuer;
  String sourceAssetName;

  String sourceAccount;
  String createdAt;

  String transactionHash;
  String transactionMemo;
  String transactionMemoType;
  String transactionEnvelope;

  public static ObservedPayment fromPaymentOperationResponse(PaymentOperationResponse paymentOp) {
    String assetCode = null, assetIssuer = null;
    if (paymentOp.getAsset() instanceof AssetTypeCreditAlphaNum) {
      AssetTypeCreditAlphaNum issuedAsset = (AssetTypeCreditAlphaNum) paymentOp.getAsset();
      assetCode = issuedAsset.getCode();
      assetIssuer = issuedAsset.getIssuer();
    }

    return ObservedPayment.builder()
        .id(paymentOp.getId().toString())
        .type(Type.PAYMENT)
        .from(paymentOp.getFrom())
        .to(paymentOp.getTo())
        .amount(paymentOp.getAmount())
        .assetType(paymentOp.getAsset().getType())
        .assetCode(assetCode)
        .assetIssuer(assetIssuer)
        .assetName(paymentOp.getAsset().toString())
        .sourceAccount(paymentOp.getSourceAccount())
        .createdAt(paymentOp.getCreatedAt())
        .transactionHash(paymentOp.getTransactionHash())
        .transactionMemo(getMemoHash(paymentOp))
        .transactionMemoType(MemoHelper.memoType(MemoType.MEMO_HASH))
        .transactionEnvelope(paymentOp.getTransaction().get().getEnvelopeXdr())
        .build();
  }

  public static ObservedPayment fromPathPaymentOperationResponse(
      PathPaymentBaseOperationResponse pathPaymentOp) {
    String assetCode = null, assetIssuer = null;
    if (pathPaymentOp.getAsset() instanceof AssetTypeCreditAlphaNum) {
      AssetTypeCreditAlphaNum issuedAsset = (AssetTypeCreditAlphaNum) pathPaymentOp.getAsset();
      assetCode = issuedAsset.getCode();
      assetIssuer = issuedAsset.getIssuer();
    }

    String sourceAssetCode = null, sourceAssetIssuer = null;
    if (pathPaymentOp.getSourceAsset() instanceof AssetTypeCreditAlphaNum) {
      AssetTypeCreditAlphaNum sourceIssuedAsset =
          (AssetTypeCreditAlphaNum) pathPaymentOp.getSourceAsset();
      sourceAssetCode = sourceIssuedAsset.getCode();
      sourceAssetIssuer = sourceIssuedAsset.getIssuer();
    }

    return ObservedPayment.builder()
        .id(pathPaymentOp.getId().toString())
        .type(Type.PATH_PAYMENT)
        .from(pathPaymentOp.getFrom())
        .to(pathPaymentOp.getTo())
        .amount(pathPaymentOp.getAmount())
        .assetType(pathPaymentOp.getAsset().getType())
        .assetCode(assetCode)
        .assetIssuer(assetIssuer)
        .assetName(pathPaymentOp.getAsset().toString())
        .sourceAmount(pathPaymentOp.getSourceAmount())
        .sourceAssetType(pathPaymentOp.getSourceAsset().getType())
        .sourceAssetCode(sourceAssetCode)
        .sourceAssetIssuer(sourceAssetIssuer)
        .sourceAssetName(pathPaymentOp.getSourceAsset().toString())
        .sourceAccount(pathPaymentOp.getSourceAccount())
        .createdAt(pathPaymentOp.getCreatedAt())
        .transactionHash(pathPaymentOp.getTransactionHash())
        .transactionMemo(getMemoHash(pathPaymentOp))
        .transactionMemoType(MemoHelper.memoType(MemoType.MEMO_HASH))
        .transactionEnvelope(pathPaymentOp.getTransaction().get().getEnvelopeXdr())
        .build();
  }

  private static String getMemoHash(OperationResponse opResponse) {
    try {
      MemoHash memoHash = (MemoHash) opResponse.getTransaction().get().getMemo();
      return new String(Base64.encodeBase64(memoHash.getBytes()));
    } catch (Exception e) {
      Log.error("Error parsing memo to MemoHash object");
      return null;
    }
  }

  public enum Type {
    @SerializedName("payment")
    PAYMENT("payment"),

    @SerializedName("path_payment")
    PATH_PAYMENT("path_payment");

    private final String name;

    Type(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
