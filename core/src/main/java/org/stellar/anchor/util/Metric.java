package org.stellar.anchor.util;

import java.util.Arrays;

public abstract class Metric {
  private static Metric metricImpl;

  public static void register(Metric metric) {
    metricImpl = metric;
  }

  public static <T extends Number> T gauge(T number, String name, String... tags) {
    if (metricImpl != null) {
      return metricImpl._gauge(number, name, tags);
    }
    return null;
  }

  public static <T extends Number> T counter(String name, String... tags) {
    if (metricImpl != null) {
      return metricImpl._counter(name, tags);
    }
    return null;
  }

  public static <T extends Number> T counter(MetricNames metric, MetricNames... tags) {
    String[] strTags = Arrays.stream(tags).map(MetricNames::name).toArray(String[]::new);
    return counter(metric.name(), strTags);
  }

  abstract <T extends Number> T _gauge(T number, String name, String... tags);

  abstract <T extends Number> T _counter(String name, String... tags);
}
