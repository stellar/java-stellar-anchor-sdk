package org.stellar.anchor.platform.condition;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;

import java.util.List;

public class OnAnySepEnabledCondition extends AbstractOnSepsEnabled {

  @Override
  String getAnnotatingClass() {
    return ConditionalOnAnySepsEnabled.class.getName();
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
      // One enabled
      if (enabled) return ConditionOutcome.match(String.format("%s enabled", className));
    }

    // Nothing enabled.
    return ConditionOutcome.noMatch(String.format("%s disabled", className));
  }
}
