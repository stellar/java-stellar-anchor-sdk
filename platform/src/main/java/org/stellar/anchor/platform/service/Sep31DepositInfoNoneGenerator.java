package org.stellar.anchor.platform.service;

import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.shared.SepDepositInfo;
import org.stellar.anchor.sep31.Sep31DepositInfoGenerator;
import org.stellar.anchor.sep31.Sep31Transaction;

public class Sep31DepositInfoNoneGenerator implements Sep31DepositInfoGenerator {
  @Override
  public SepDepositInfo generate(Sep31Transaction txn) throws AnchorException {
    throw new BadRequestException("SEP-31 deposit info generation is disabled");
  }
}
