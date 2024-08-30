package org.stellar.anchor.client;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

public interface ClientConfig {
  String getName();

  @Deprecated
  String getCallbackUrl();

  CallbackUrls getCallbackUrls();

  @Data
  @Builder
  class CallbackUrls {
    private String sep6;
    private String sep24;
    private String sep31;
    private String sep12;
  }

  /**
   * Returns true if any of the callback URLs are set.
   *
   * @return true if any of the callback URLs are set
   */
  default boolean isCallbackEnabled() {
    return !StringUtils.isEmpty(getCallbackUrl())
        || (getCallbackUrls() != null
            && (!StringUtils.isEmpty(getCallbackUrls().getSep6())
                || !StringUtils.isEmpty(getCallbackUrls().getSep24())
                || !StringUtils.isEmpty(getCallbackUrls().getSep31())
                || !StringUtils.isEmpty(getCallbackUrls().getSep12())));
  }
}
