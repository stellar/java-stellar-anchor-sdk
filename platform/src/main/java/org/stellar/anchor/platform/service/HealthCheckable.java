package org.stellar.anchor.platform.service;

import org.stellar.anchor.api.platform.HealthCheckResult;

import java.util.List;

public interface HealthCheckable extends Comparable<HealthCheckable> {
    String getName();
    List<String> getTags();

    HealthCheckResult check(HealthCheckContext context);

}
