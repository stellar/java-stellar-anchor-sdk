package org.stellar.anchor.sep6;

import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.shared.SepDepositInfo;

public interface Sep6DepositInfoGenerator {

  /**
   * Gets the deposit info based on the input parameter.
   *
   * @param txn the original SEP-6 transaction the deposit info will be used for.
   * @return a SepDepositInfo instance containing the destination address, memo and memoType.
   * @throws AnchorException if the deposit info cannot be generated
   */
  SepDepositInfo generate(Sep6Transaction txn) throws AnchorException;
}
