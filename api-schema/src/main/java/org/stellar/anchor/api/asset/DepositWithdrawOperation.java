package org.stellar.anchor.api.asset;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class DepositWithdrawOperation {
  Boolean enabled = false;

  @SerializedName("min_amount")
  Long minAmount;

  @SerializedName("max_amount")
  Long maxAmount;

  List<String> methods;
}
