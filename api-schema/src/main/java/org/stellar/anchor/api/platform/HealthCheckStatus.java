package org.stellar.anchor.api.platform;

public enum HealthCheckStatus {
  RED("red"),
  GREEN("green");

  String name;

  HealthCheckStatus(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
