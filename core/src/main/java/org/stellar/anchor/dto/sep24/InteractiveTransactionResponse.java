package org.stellar.anchor.dto.sep24;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class InteractiveTransactionResponse {
    String type;
    String url;
    String id;
}

