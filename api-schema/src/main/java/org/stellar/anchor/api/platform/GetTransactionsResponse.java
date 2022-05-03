package org.stellar.anchor.api.platform;

import java.util.List;
import lombok.Data;

@Data
public class GetTransactionsResponse {
  List<GetTransactionResponse> records;
  String cursor;
}
