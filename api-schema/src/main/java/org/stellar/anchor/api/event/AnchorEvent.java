package org.stellar.anchor.api.event;

import com.google.gson.annotations.SerializedName;
import lombok.*;
import org.stellar.anchor.api.platform.GetQuoteResponse;
import org.stellar.anchor.api.platform.GetTransactionResponse;

/**
 * The Event object is used for event notification.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-docs/blob/main/openapi/anchor-platform/Events%20Schema.yml">Events
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
    @SerializedName("transaction_created")
    TRANSACTION_CREATED("transaction_created"),
    @SerializedName("transaction_status_changed")
    TRANSACTION_STATUS_CHANGED("transaction_status_changed"),
    @SerializedName("transaction_error")
    TRANSACTION_ERROR("transaction_error"),
    @SerializedName("quote_created")
    QUOTE_CREATED("quote_created");

    public final String type;

    Type(String type) {
      this.type = type;
    }
  }
}
