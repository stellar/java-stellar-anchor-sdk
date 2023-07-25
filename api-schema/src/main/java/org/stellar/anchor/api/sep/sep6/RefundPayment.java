package org.stellar.anchor.api.sep.sep6;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RefundPayment {
  String id;

  @SerializedName("id_type")
  String idType;

  String amount;
  String fee;
}
