package org.stellar.anchor.platform.service;

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
  List<String> getTags();

  /**
   * Perform the check.
   *
   * @return The result specific to the service.
   */
  HealthCheckResult check();
}
