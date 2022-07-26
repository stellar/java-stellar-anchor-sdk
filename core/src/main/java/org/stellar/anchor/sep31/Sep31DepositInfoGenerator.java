package org.stellar.anchor.sep31;

import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.sep.sep31.Sep31DepositInfo;

public interface Sep31DepositInfoGenerator {
  /**
   * Gets the deposit info based on the input parameter.
   *
   * @param txn the original SEP-31 transaction the deposit info will be used for.
   * @return a Sep31DepositInfo instance containing the destination address, memo and memoType.
   */
  Sep31DepositInfo generate(Sep31Transaction txn) throws AnchorException;
}
