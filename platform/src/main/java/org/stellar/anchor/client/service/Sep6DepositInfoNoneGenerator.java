package org.stellar.anchor.client.service;

import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.shared.SepDepositInfo;
import org.stellar.anchor.sep6.Sep6DepositInfoGenerator;
import org.stellar.anchor.sep6.Sep6Transaction;

public class Sep6DepositInfoNoneGenerator implements Sep6DepositInfoGenerator {
  @Override
  public SepDepositInfo generate(Sep6Transaction txn) throws AnchorException {
    throw new BadRequestException("SEP-6 deposit info generation is disabled");
  }
}
