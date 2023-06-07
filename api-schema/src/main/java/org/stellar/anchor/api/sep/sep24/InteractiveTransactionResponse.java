package org.stellar.anchor.api.sep.sep24;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * The response to the GET /transactions/deposit/interactive and /transactions/withdraw/interactive
 * endpoints of SEP-24.
 */
@AllArgsConstructor
@Data
public class InteractiveTransactionResponse {
  String type;
  String url;
  String id;
}
