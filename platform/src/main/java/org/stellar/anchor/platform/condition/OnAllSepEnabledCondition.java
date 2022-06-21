package org.stellar.anchor.platform.condition;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OnAllSepEnabledCondition extends AbstractOnSepsEnabled {
  @Override
  String getAnnotatingClass() {
    return ConditionalOnAllSepsEnabled.class.getName();
  }

  @Override
  ConditionOutcome getMatchOutcome(List<String> seps, ConditionContext context, String className) {
    for (String sep : seps) {
      boolean enabled =
          "true"
              .equalsIgnoreCase(
                  context
                      .getEnvironment()
                      .getProperty(String.format("%s.enabled", sep.toLowerCase())));
      // One is not enabled.
      if (!enabled) return ConditionOutcome.noMatch(String.format("%s disabled", className));
    }

    // All enabled.
    return ConditionOutcome.match(String.format("%s enabled", className));
  }
}
