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

public class OnSepEnabledCondition extends SpringBootCondition {
  @Override
  public ConditionOutcome getMatchOutcome(
      ConditionContext context, AnnotatedTypeMetadata metadata) {

    // Gets the annotated classname
    String className =
        metadata.getAnnotations().stream()
            .map(annotation -> Objects.requireNonNull(annotation.getSource()).toString())
            .distinct()
            .findAny()
            .orElse("");

    // Find all annotation attributes
    List<AnnotationAttributes> allAnnotationAttributes =
        metadata.getAnnotations().stream(ConditionalOnSepsEnabled.class.getName())
            .filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
            .map(MergedAnnotation::asAnnotationAttributes)
            .collect(Collectors.toList());

    for (AnnotationAttributes annotationAttributes : allAnnotationAttributes) {
      // if any sep is enabled in the "seps" value, return a match.
      String[] seps = (String[]) annotationAttributes.get("seps");
      for (String sep : seps) {
        boolean enabled =
            Boolean.parseBoolean(
                context
                    .getEnvironment()
                    .getProperty(String.format("%s.enabled", sep.toLowerCase())));
        if (enabled) return ConditionOutcome.match(String.format("%s enabled", className));
      }
    }

    // Nothing matched.
    return ConditionOutcome.noMatch(String.format("%s disabled", className));
  }
}
