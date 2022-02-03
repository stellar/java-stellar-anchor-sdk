package org.stellar.anchor.dto.sep24;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class GetTransactionsResponse {
  List<TransactionResponse> transactions = new ArrayList<>();
}
