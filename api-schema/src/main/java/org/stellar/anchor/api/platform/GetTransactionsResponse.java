package org.stellar.anchor.api.platform;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetTransactionsResponse {
  List<GetTransactionResponse> records;
}
