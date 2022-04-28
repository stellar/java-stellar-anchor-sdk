package org.stellar.anchor.api.sep.sep24;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class GetTransactionsResponse {
  List<TransactionResponse> transactions = new ArrayList<>();
}
