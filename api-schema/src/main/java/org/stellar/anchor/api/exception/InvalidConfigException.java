package org.stellar.anchor.api.exception;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/** Thrown when the server configuration is invalid. */
@Getter
public class InvalidConfigException extends AnchorException {
  final List<String> messages;

  public InvalidConfigException(String... messages) {
    this(Arrays.asList(messages), null);
  }

  public InvalidConfigException(List<String> messages) {
    this(messages, null);
  }

  public InvalidConfigException(List<String> messages, Exception cause) {
    super(StringUtils.join(messages, System.lineSeparator()), cause);
    this.messages = messages;
  }
}
