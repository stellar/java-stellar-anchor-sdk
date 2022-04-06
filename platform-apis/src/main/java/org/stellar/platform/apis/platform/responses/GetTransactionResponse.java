package org.stellar.platform.apis.platform.responses;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.stellar.platform.apis.shared.Transaction;

@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
public class GetTransactionResponse extends Transaction {}
