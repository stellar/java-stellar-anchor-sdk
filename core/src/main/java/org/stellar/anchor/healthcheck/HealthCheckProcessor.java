package org.stellar.anchor.healthcheck;

import java.util.*;
import org.stellar.anchor.api.platform.HealthCheckResponse;
import org.stellar.anchor.api.platform.HealthCheckResult;

public class HealthCheckProcessor {
  private final Map<String, List<HealthCheckable>> mapCheckable = new HashMap<>();

  public HealthCheckProcessor(List<HealthCheckable> checkables) {
    for (HealthCheckable checkable : checkables) {
      for (HealthCheckable.Tags tag : checkable.getTags()) {
        List<HealthCheckable> checksOfTag =
            mapCheckable.computeIfAbsent(tag.toString(), k -> new ArrayList<>());
        checksOfTag.add(checkable);
      }
    }
  }

  public HealthCheckResponse check(List<String> checkTags) {
    HealthCheckResponse healthCheckResponse = new HealthCheckResponse();
    SortedSet<HealthCheckable> checkSet = new TreeSet<>();
    for (String checkTag : checkTags) {
      List<HealthCheckable> checkables = mapCheckable.get(checkTag);
      if (checkables != null) checkSet.addAll(checkables);
    }

    List<HealthCheckResult> results = new ArrayList<>(checkSet.size());
    for (HealthCheckable checkable : checkSet) {
      HealthCheckResult result = checkable.check();
      results.add(result);
    }

    return healthCheckResponse.complete(results);
  }
}
