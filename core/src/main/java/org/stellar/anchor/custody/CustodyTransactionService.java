package org.stellar.anchor.custody;

import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.sep31.Sep31Transaction;

public interface CustodyTransactionService {

  void create(Sep24Transaction txn) throws AnchorException;

  void create(Sep31Transaction txn) throws AnchorException;
}
