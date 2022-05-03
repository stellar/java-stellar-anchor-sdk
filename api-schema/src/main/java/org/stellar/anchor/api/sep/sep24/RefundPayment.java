package org.stellar.anchor.api.sep.sep24;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RefundPayment {
  String id;

  @SerializedName("id_type")
  String idType;

  String amount;
  String fee;
}
