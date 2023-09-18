package org.stellar.anchor.platform.service;

import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.shared.SepDepositInfo;
import org.stellar.anchor.sep24.Sep24DepositInfoGenerator;
import org.stellar.anchor.sep24.Sep24Transaction;

public class Sep24DepositInfoNoneGenerator implements Sep24DepositInfoGenerator {

  @Override
  public SepDepositInfo generate(Sep24Transaction txn) throws BadRequestException {
    throw new BadRequestException("SEP-24 deposit info generation is disabled");
  }
}
