package org.stellar.anchor.api.platform;

import com.google.gson.annotations.SerializedName;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class HealthCheckResponse {
  final Instant started;

  @SerializedName("elapsed_time_ms")
  Duration elapsedTime = Duration.ZERO;

  @SerializedName("number_of_checks")
  int numberOfChecks;

  Map<String, HealthCheckResult> checks;

  public HealthCheckResponse() {
    this.started = Instant.now();
  }

  public HealthCheckResponse complete(List<HealthCheckResult> results) {
    checks = new HashMap<>();
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
