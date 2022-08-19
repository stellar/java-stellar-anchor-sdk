package org.stellar.anchor.api.exception;

import java.util.List;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class InvalidConfigException extends AnchorException {
  List<String> messages;

  public InvalidConfigException(String message, Exception cause) {
    super(message, cause);
  }

  public InvalidConfigException(String message) {
    super(message);
    messages = List.of(message);
  }

  public InvalidConfigException(List<String> messages) {
    super(StringUtils.join(messages, System.lineSeparator()));
    this.messages = messages;
  }
}
