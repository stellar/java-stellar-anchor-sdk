package org.stellar.anchor.sep31;

import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.SepHelper.validateTransactionStatus;

import org.stellar.anchor.exception.BadRequestException;
import org.stellar.anchor.model.Sep31Transaction;

public class Sep31Helper {
  public static boolean allAmountAvailable(Sep31Transaction txn) {
    return txn.getAmountIn() != null
        && txn.getAmountInAsset() != null
        && txn.getAmountFee() != null
        && txn.getAmountFeeAsset() != null
        && txn.getAmountOut() != null
        && txn.getAmountOutAsset() != null;
  }

  public static boolean amountEquals(String amount1, String amount2) {
    return decimal(amount1).compareTo(decimal(amount2)) == 0;
  }

  public static void validateStatus(Sep31Transaction txn) throws BadRequestException {
    if (!validateTransactionStatus(txn.getStatus(), 31)) {
      throw new BadRequestException(
          String.format("'%s' is not a valid status of SEP31.", txn.getStatus()));
    }
  }
}
