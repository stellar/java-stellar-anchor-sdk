package org.stellar.anchor.api.callback;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** SendEventResponse is the response body for the /event Callback API endpoint. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendEventResponse {
  /** The message of the response. */
  String message;
}
