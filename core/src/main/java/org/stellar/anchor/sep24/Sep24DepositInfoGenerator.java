package org.stellar.anchor.sep24;

import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.shared.SepDepositInfo;

public interface Sep24DepositInfoGenerator {

  /**
   * Gets the deposit info based on the input parameter.
   *
   * @param txn the original SEP-24 transaction the deposit info will be used for.
   * @return a SepDepositInfo instance containing the destination address, memo and memoType.
   * @throws AnchorException if the deposit info cannot be generated
   */
  SepDepositInfo generate(Sep24Transaction txn) throws AnchorException;
}
