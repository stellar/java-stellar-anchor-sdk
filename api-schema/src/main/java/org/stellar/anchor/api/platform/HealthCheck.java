package org.stellar.anchor.api.platform;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class HealthCheck {
  Instant started;

  @SerializedName("elapsed_time")
  Duration elapsedTime;

  @SerializedName("number_of_checks")
  int numberOfChecks;
  Map<String, HealthCheckResult> checks = new HashMap<>();

  public HealthCheck() {
    this.started = Instant.now();
  }

  public HealthCheck finish(List<HealthCheckResult> results) {
    for (HealthCheckResult result : results) {
      checks.put(result.name(), result);
    }
    numberOfChecks = checks.size();
    elapsedTime = Duration.between(started, Instant.now());
    return this;
  }

  @SerializedName("size")
  public int getSize() {
    return checks.size();
  }
}
