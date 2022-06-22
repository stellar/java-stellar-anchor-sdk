package org.stellar.anchor.platform.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

/** Indicates if any of the SEPs in the "seps" list is enabled in the configuration. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnAllSepEnabledCondition.class)
public @interface ConditionalOnAllSepsEnabled {
  /**
   * The list of seps that are checked if enabled.
   *
   * @return The list of seps
   */
  String[] seps() default {};
}
