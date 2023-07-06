package org.stellar.anchor.api.sep.operation;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class Sep6Operation {
  @SerializedName("deposit")
  DepositOperation deposit;

  @SerializedName("withdraw")
  WithdrawOperation withdraw;

  @Data
  public static class DepositOperation {
    @SerializedName("exchange_enabled")
    boolean exchangeEnabled;

    @SerializedName("fields")
    Map<String, FieldSpec> fields;
  }

  @Data
  public static class WithdrawOperation {
    @SerializedName("withdraw_enabled")
    boolean exchangeEnabled;

    @SerializedName("types")
    List<WithdrawType> types;
  }

  @Data
  public static class WithdrawType {
    @SerializedName("type")
    String type;

    @SerializedName("fields")
    Map<String, FieldSpec> fields;
  }
}
