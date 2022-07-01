package org.stellar.anchor.platform.condition;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.type.AnnotatedTypeMetadata;

public abstract class AbstractOnSepsEnabled extends SpringBootCondition {
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
        metadata.getAnnotations().stream(getAnnotatingClass())
            .filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
            .map(MergedAnnotation::asAnnotationAttributes)
            .collect(Collectors.toList());

    LinkedList<String> seps = new LinkedList<>();
    for (AnnotationAttributes annotationAttributes : allAnnotationAttributes) {
      for (String sep : (String[]) annotationAttributes.get("seps")) {
        seps.add(sep);
      }
    }

    return getMatchOutcome(seps, context, className);
  }

  abstract String getAnnotatingClass();

  abstract ConditionOutcome getMatchOutcome(
      List<String> seps, ConditionContext context, String className);
}
