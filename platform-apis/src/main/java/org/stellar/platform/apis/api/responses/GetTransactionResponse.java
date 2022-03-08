package org.stellar.platform.apis.api.responses;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.stellar.platform.apis.shared.Transaction;

@EqualsAndHashCode(callSuper = false)
@Data
public class GetTransactionResponse extends Transaction {}
