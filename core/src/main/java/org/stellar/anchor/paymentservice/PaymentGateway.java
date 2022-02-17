package org.stellar.anchor.paymentservice;

import java.util.HashMap;
import java.util.Map;

public class PaymentGateway {
  private final Map<String, PaymentService> services;

  public PaymentGateway(Map<String, PaymentService> services) {
    this.services = services;
  }

  public String getServices() {
    // TODO: This is a demo function. Remove this method.
    return String.join(",", services.keySet());
  }

  public PaymentService getService(String serviceName) {
    return services.get(serviceName);
  }

  public static class Builder {
    HashMap<String, PaymentService> map = new HashMap<>();

    public PaymentGateway build() {
      return new PaymentGateway(map);
    }

    public Builder add(PaymentService service) {
      if (map.get(service.getName()) != null) {
        throw new RuntimeException(
            "The serivce with the name [" + service.getName() + "] already exists.");
      }
      map.put(service.getName(), service);
      return this;
    }
  }
}
