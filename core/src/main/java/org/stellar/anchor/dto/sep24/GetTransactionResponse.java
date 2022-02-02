package org.stellar.anchor.dto.sep24;

import lombok.Data;

@Data
public class GetTransactionResponse {
    TransactionResponse transaction;

    public static GetTransactionResponse of(TransactionResponse tr) {
        GetTransactionResponse gtr = new GetTransactionResponse();
        gtr.transaction = tr;
        return gtr;
    }
}
