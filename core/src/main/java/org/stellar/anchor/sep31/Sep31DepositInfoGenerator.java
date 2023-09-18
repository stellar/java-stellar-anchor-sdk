package org.stellar.anchor.sep31;

import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.shared.SepDepositInfo;

public interface Sep31DepositInfoGenerator {

  /**
   * Gets the deposit info based on the input parameter.
   *
   * @param txn the original SEP-31 transaction the deposit info will be used for.
   * @return a SepDepositInfo instance containing the destination address, memo and memoType.
   * @throws AnchorException if the deposit info cannot be generated
   */
  SepDepositInfo generate(Sep31Transaction txn) throws AnchorException;
}
