package org.stellar.anchor.platform.payment.observer.circle.model.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CircleConfigurationResponse
    extends CircleDetailResponse<CircleConfigurationResponse.Data> {
  @lombok.Data
  public static class Data {
    public Payments payments;
  }

  @lombok.Data
  public static class Payments {
    public String masterWalletId;
  }
}
