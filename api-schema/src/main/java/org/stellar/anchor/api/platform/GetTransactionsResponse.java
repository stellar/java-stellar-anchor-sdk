package org.stellar.anchor.api.platform;

import java.util.List;
import lombok.Data;
import org.stellar.anchor.api.shared.Transaction;

@Data
public class GetTransactionsResponse {
  List<Transaction> records;
  String cursor;
}
