package org.stellar.anchor.api.platform;

import java.util.List;

public interface HealthCheckResult {
  String name();

  /**
   * List all health check statuses.
   *
   * @return the list of health check status.
   */
  List<HealthCheckStatus> getStatuses();

  /**
   * the current status of the health check result.
   *
   * @return the status.
   */
  HealthCheckStatus getStatus();
}
