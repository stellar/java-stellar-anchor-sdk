package org.stellar.anchor.util;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import java.util.Arrays;

public class Metric {
  private static MetricImpl metricImpl = new MicroMeterMetric();

  /**
   * Registers the implementation of the metric. Default implementation is MicroMeterMetric.
   *
   * @param metric The metric implementation.
   */
  public static void register(MetricImpl metric) {
    metricImpl = metric;
  }

  /**
   * Sets and returns the current value of the gauge with the given name.
   *
   * @param metric The name of the metric.
   * @param number The value of the metric.
   * @param tags The tags of the metric.
   * @return The value of the metric.
   * @param <T> The type of the number of the metric.
   */
  public static <T extends Number> T gauge(MetricName metric, T number, MetricName... tags) {
    if (metricImpl != null) {
      return metricImpl._gauge(number, metric, tags);
    }
    return null;
  }

  public static <T extends Number> void counter(
      MetricName metric, T increment, MetricName... tags) {
    if (metricImpl != null) {
      metricImpl._counter(metric, increment, tags);
    }
  }

  /**
   * Increments the counter of a metric with the name by 1.
   *
   * @param metric The name of the metric.
   * @param tags The tags of the metric.
   */
  public static void counter(MetricName metric, MetricName... tags) {
    if (metricImpl != null) {
      metricImpl._counter(metric, tags);
    }
  }
}

interface MetricImpl {
  /**
   * Sets and returns the current value of the gauge with the given name.
   *
   * @param number The value of the metric.
   * @param name The name of the metric.
   * @param tags The tags of the metric.
   * @return The value of the metric.
   * @param <T> The type of the number of the metric.
   */
  <T extends Number> T _gauge(T number, MetricName name, MetricName... tags);

  /**
   * Increments the counter of a metric with the name.
   *
   * @param name The name of the metric.
   * @param tags The tags of the metric.
   */
  default void _counter(MetricName name, MetricName... tags) {
    _counter(name, 1, tags);
  }

  /**
   * Increments the counter of a metric with the name by the increment.
   *
   * @param name The name of the metric.
   * @param increment The increment of the metric.
   * @param tags The tags of the metric.
   * @param <T> The type of the number of the metric.
   */
  <T extends Number> void _counter(MetricName name, T increment, MetricName... tags);
}

/** Implementation of the metric using MicroMeter. */
class MicroMeterMetric implements MetricImpl {
  static {
    Metric.register(new MicroMeterMetric());
  }

  @Override
  public <T extends Number> T _gauge(T number, MetricName metric, MetricName... tags) {
    return Metrics.gauge(metric.name().toLowerCase(), Tags.of(toStringArray(tags)), number);
  }

  @Override
  public <T extends Number> void _counter(MetricName metric, T increment, MetricName... tags) {
    Metrics.counter(metric.name().toLowerCase(), toStringArray(tags))
        .increment(increment.doubleValue());
  }

  String[] toStringArray(MetricName... tags) {
    return Arrays.stream(tags)
        .map(MetricName::name)
        .map(String::toLowerCase)
        .toArray(String[]::new);
  }
}
