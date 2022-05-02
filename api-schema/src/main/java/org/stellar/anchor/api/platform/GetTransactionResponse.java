package org.stellar.anchor.api.platform;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.stellar.anchor.api.shared.Transaction;

@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
public class GetTransactionResponse extends Transaction {}
