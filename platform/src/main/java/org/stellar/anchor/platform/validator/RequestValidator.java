package org.stellar.anchor.platform.validator;

import static java.util.stream.Collectors.joining;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;

public class RequestValidator {

  private final Validator validator;

  public RequestValidator(Validator validator) {
    this.validator = validator;
  }

  public <T> void validate(T request) throws InvalidParamsException {
    Set<ConstraintViolation<T>> violations = validator.validate(request);
    if (CollectionUtils.isNotEmpty(violations)) {
      throw new InvalidParamsException(
          violations.stream()
              .map(ConstraintViolation::getMessage)
              .collect(joining(System.lineSeparator())));
    }
  }
}
