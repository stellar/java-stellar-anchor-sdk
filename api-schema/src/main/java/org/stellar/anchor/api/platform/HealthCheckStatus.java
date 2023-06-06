package org.stellar.anchor.api.platform;

/** The status of the /health check response body. */
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
