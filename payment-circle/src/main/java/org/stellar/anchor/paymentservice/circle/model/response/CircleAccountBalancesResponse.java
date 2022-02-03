package org.stellar.anchor.paymentservice.circle.model.response;

import java.util.List;
import lombok.Data;
import org.stellar.anchor.paymentservice.circle.model.CircleBalance;

@Data
public class CircleAccountBalancesResponse {
  Data data;

  @lombok.Data
  public static class Data {
    public List<CircleBalance> available;
    public List<CircleBalance> unsettled;
  }
}
