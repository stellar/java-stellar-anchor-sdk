package org.stellar.platform.apis.platform.responses;

import java.util.List;
import lombok.Data;
import org.stellar.platform.apis.shared.Transaction;

@Data
public class GetTransactionsResponse {
  List<Transaction> records;
  String cursor;
}
