package org.stellar.anchor.platform.custody.fireblocks;

import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest;
import org.stellar.anchor.api.exception.custody.CustodyBadRequestException;
import org.stellar.anchor.platform.custody.CustodyPaymentService;
import org.stellar.anchor.platform.custody.CustodyTransactionService;
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo;
import org.stellar.anchor.util.MemoHelper;
import org.stellar.sdk.xdr.MemoType;

public class FireblocksCustodyTransactionService extends CustodyTransactionService {

  public FireblocksCustodyTransactionService(
      JdbcCustodyTransactionRepo custodyTransactionRepo,
      CustodyPaymentService<?> custodyPaymentService) {
    super(custodyTransactionRepo, custodyPaymentService);
  }

  @Override
  protected void validateRequest(CreateCustodyTransactionRequest request)
      throws CustodyBadRequestException {
    // Fireblocks doesn't support memo of type HASH.
    final String hashMemoType = MemoHelper.memoTypeAsString(MemoType.MEMO_HASH);
    if (hashMemoType.equals(request.getMemoType())) {
      throw new CustodyBadRequestException(
          String.format(
              "Memo type [%s] is not supported by Fireblocks custody service", hashMemoType));
    }
  }
}
