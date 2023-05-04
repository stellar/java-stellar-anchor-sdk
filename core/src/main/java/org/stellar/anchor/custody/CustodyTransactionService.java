package org.stellar.anchor.custody;

import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.sep31.Sep31Transaction;

public interface CustodyTransactionService {

  /**
   * Create custody transaction for SEP24 transaction. Transaction will be created only if custody
   * type is not `none`
   *
   * @param txn SEP24 transaction
   * @throws AnchorException if error happens
   */
  void create(Sep24Transaction txn) throws AnchorException;

  /**
   * Create custody transaction for SEP31 transaction. Transaction will be created only if custody
   * type is not `none`
   *
   * @param txn SEP31 transaction
   * @throws AnchorException if error happens
   */
  void create(Sep31Transaction txn) throws AnchorException;
}
