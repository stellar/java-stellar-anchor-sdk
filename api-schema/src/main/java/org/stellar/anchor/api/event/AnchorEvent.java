package org.stellar.anchor.api.event;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;
import org.stellar.anchor.api.platform.GetQuoteResponse;
import org.stellar.anchor.api.platform.GetTransactionResponse;

/**
 * The Event object is used for event notification.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/ap/Events%20Schema.yml">Events
 *     Schema</a>
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnchorEvent {
  Type type;
  String id;
  String sep;
  GetTransactionResponse transaction;
  GetQuoteResponse quote;

  public enum Type {
    TRANSACTION_CREATED("transaction_created"),
    TRANSACTION_STATUS_CHANGED("transaction_status_changed"),
    @SuppressWarnings("unused")
    TRANSACTION_ERROR("transaction_error"),
    QUOTE_CREATED("quote_created");

    @JsonValue public final String type;

    Type(String type) {
      this.type = type;
    }
  }
}
