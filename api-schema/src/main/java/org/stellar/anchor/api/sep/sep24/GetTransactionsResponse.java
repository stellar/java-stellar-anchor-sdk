package org.stellar.anchor.api.sep.sep24;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** The response to the GET /transactions endpoint of SEP-24. */
@Data
public class GetTransactionsResponse {
  List<TransactionResponse> transactions = new ArrayList<>();
}
