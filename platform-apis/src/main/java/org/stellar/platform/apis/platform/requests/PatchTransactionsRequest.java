package org.stellar.platform.apis.platform.requests;

import lombok.Data;

import java.util.List;

@Data
public class PatchTransactionsRequest {
  List<PatchTransactionRequest> records;
}
