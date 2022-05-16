package org.stellar.anchor.platform.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.platform.HealthCheckResponse;
import org.stellar.anchor.api.platform.HealthCheckResult;

@Service
public class HealthCheckService {
  private final Map<String, List<HealthCheckable>> mapCheckable = new HashMap<>();

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

  public HealthCheckResponse check(List<String> checkTags) {
    HealthCheckResponse healthCheckResponse = new HealthCheckResponse();
    SortedSet<HealthCheckable> checkSet = new TreeSet<>();
    for (String checkTag : checkTags) {
      List<HealthCheckable> checkables = mapCheckable.get(checkTag);
      checkSet.addAll(checkables);
    }

    List<HealthCheckResult> results = new ArrayList<>(checkSet.size());
    for (HealthCheckable checkable : checkSet) {
      HealthCheckResult result = checkable.check();
      results.add(result);
    }

    return healthCheckResponse.complete(results);
  }
}
