package org.stellar.anchor.paymentservice.circle.model.response;

import lombok.Data;

@Data
public class CircleConfigurationResponse {
  public Data data;

  @lombok.Data
  public static class Data {
    public Payments payments;
  }

  @lombok.Data
  public static class Payments {
    public String masterWalletId;
  }
}
