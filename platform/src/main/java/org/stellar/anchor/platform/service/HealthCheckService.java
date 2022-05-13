package org.stellar.anchor.platform.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.platform.HealthCheck;
import org.stellar.anchor.api.platform.HealthCheckResult;

@Service
public class HealthCheckService implements HealthCheckContext {
  private Map<String, List<HealthCheckable>> mapCheckable = new HashMap<>();

  HealthCheckService(List<HealthCheckable> checkables) {
    for (HealthCheckable checkable : checkables) {
      for (String tag : checkable.getTags()) {
        List<HealthCheckable> checksOfTag = mapCheckable.get(tag);
        if (checksOfTag == null) {
          checksOfTag = List.of(checkable);
          mapCheckable.put(tag, checksOfTag);
        } else {
          checksOfTag.add(checkable);
        }
      }
    }
  }

  public HealthCheck check(List<String> checkTags) {
    HealthCheck healthCheck = new HealthCheck();
    SortedSet<HealthCheckable> checkSet = new TreeSet<>();
    for (String checkTag : checkTags) {
      List<HealthCheckable> checkables = mapCheckable.get(checkTag);
      checkSet.addAll(checkables);
    }

    List<HealthCheckResult> results = new ArrayList<>(checkSet.size());
    for (HealthCheckable checkable : checkSet) {
      HealthCheckResult result = checkable.check(this);
      results.add(result);
    }

    return healthCheck.finish(results);
  }
}
