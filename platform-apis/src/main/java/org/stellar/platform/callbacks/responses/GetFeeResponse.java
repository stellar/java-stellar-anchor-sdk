package org.stellar.platform.callbacks.responses;

import lombok.Data;

@Data
public class GetFeeResponse {
  Amount fee;
}

@Data
class Amount {
  String amount;
  String asset;
}
