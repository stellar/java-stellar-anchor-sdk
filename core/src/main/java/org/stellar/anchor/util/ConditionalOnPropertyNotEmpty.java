package org.stellar.anchor.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(ConditionalOnPropertyNotEmpty.OnPropertyNotEmptyCondition.class)
public @interface ConditionalOnPropertyNotEmpty {
  String value();

  class OnPropertyNotEmptyCondition implements Condition {

    @Override
    public boolean matches(@NotNull ConditionContext context, AnnotatedTypeMetadata metadata) {
      Map<String, Object> attrs =
          metadata.getAnnotationAttributes(ConditionalOnPropertyNotEmpty.class.getName());
      if (attrs == null) {
        return false;
      }
      String propertyName = (String) attrs.get("value");
      String val = context.getEnvironment().getProperty(propertyName);
      return val != null && !val.trim().isEmpty();
    }
  }
}
