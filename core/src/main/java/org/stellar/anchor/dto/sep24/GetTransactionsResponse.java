package org.stellar.anchor.dto.sep24;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GetTransactionsResponse {
    List<TransactionResponse> transactions = new ArrayList<>();
}
