package org.stellar.anchor.platform.config;

import lombok.Data;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.CircleConfig;
@Data
public class PropertyCircleConfig implements CircleConfig {
  String circleUrl;

  String apiKey;

  public BindException validate() {
    /*Errors errors = new BindException(this, "circleConfig");
    ValidationUtils.invokeValidator(this, this, errors);
    return !errors.hasErrors();*/
    BindException errors = new BindException(this, "circleConfig");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "circleUrl", "empty-circleUrl");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "apiKey", "empty-apiKey");
    //return !errors.hasErrors();
    return errors;
  }

  /*@Override
  public boolean supports(Class<?> clazz) {
    return CircleConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    if (i am not needed) {
      return;
    }

  }*/
}
