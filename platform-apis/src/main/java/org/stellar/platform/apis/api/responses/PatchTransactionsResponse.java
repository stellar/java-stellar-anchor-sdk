package org.stellar.platform.apis.api.responses;

import java.util.List;
import lombok.Data;
import org.stellar.platform.apis.shared.Transaction;

@Data
public class PatchTransactionsResponse {
  List<Transaction> records;
}
