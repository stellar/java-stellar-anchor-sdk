package org.stellar.anchor.api.platform;

public enum HealthCheckStatus {
  RED("red"),
  YELLOW("yellow"),
  GREEN("green");

  final String name;

  HealthCheckStatus(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
