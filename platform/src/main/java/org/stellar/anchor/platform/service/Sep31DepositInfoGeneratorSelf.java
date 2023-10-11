package org.stellar.anchor.platform.service;

import static org.stellar.anchor.util.MemoHelper.memoTypeAsString;
import static org.stellar.sdk.xdr.MemoType.MEMO_HASH;

import java.util.Base64;
import org.apache.commons.lang3.StringUtils;
import org.stellar.anchor.api.sep.sep31.Sep31DepositInfo;
import org.stellar.anchor.sep31.Sep31DepositInfoGenerator;
import org.stellar.anchor.sep31.Sep31Transaction;

public class Sep31DepositInfoGeneratorSelf implements Sep31DepositInfoGenerator {
  @Override
  public Sep31DepositInfo generate(Sep31Transaction txn) {
    String memo = StringUtils.truncate(txn.getId(), 32);
    memo = StringUtils.leftPad(memo, 32, '0');
    memo = new String(Base64.getEncoder().encode(memo.getBytes()));

    return new Sep31DepositInfo(txn.getStellarAccountId(), memo, memoTypeAsString(MEMO_HASH));
  }
}
