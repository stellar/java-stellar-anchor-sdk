package org.stellar.anchor.platform.payment.observer.circle.model.response;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.stellar.anchor.platform.payment.observer.circle.model.CircleBalance;

@EqualsAndHashCode(callSuper = true)
@Data
public class CircleAccountBalancesResponse
    extends CircleDetailResponse<CircleAccountBalancesResponse.Data> {
  @lombok.Data
  public static class Data {
    public List<CircleBalance> available;
    public List<CircleBalance> unsettled;
  }
}
