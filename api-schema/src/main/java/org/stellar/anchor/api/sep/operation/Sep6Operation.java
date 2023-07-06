package org.stellar.anchor.api.sep.operation;

import com.google.gson.annotations.SerializedName;
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
    Map<String, Field> fields;
  }

  @Data
  public static class WithdrawOperation {
    @SerializedName("exchange_enabled")
    boolean exchangeEnabled;

    @SerializedName("types")
    Map<String, WithdrawFields> types;
  }

  @Data
  public static class WithdrawFields {
    Map<String, Field> fields;
  }
}
