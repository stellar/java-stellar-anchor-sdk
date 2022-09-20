package org.stellar.anchor.healthcheck;

import java.util.List;
import org.stellar.anchor.api.platform.HealthCheckResult;

/**
 * The interface is for HealthCheckService to request the health checks for services that implements
 * this interface.
 */
public interface HealthCheckable extends Comparable<HealthCheckable> {
  /**
   * The name of the service.
   *
   * @return the name.
   */
  String getName();

  /**
   * The tags of the service. The HealthCheckService receives a list of tags in the health check
   * request. The check function will be called if any of the requested tags maches the list of tags
   * returned by the interface.
   *
   * @return the tags
   */
  List<Tags> getTags();

  /**
   * Perform the check.
   *
   * @return The result specific to the service.
   */
  HealthCheckResult check();

  enum Tags {
    ALL("all"),
    KAFKA("kafka"),
    EVENT("evnet"),
    CONFIG("config");

    private String name;

    Tags(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
