package org.stellar.anchor.client.service;

import static org.stellar.anchor.util.MemoHelper.memoTypeAsString;
import static org.stellar.sdk.xdr.MemoType.MEMO_HASH;

import java.util.Base64;
import org.apache.commons.lang3.StringUtils;
import org.stellar.anchor.api.shared.SepDepositInfo;
import org.stellar.anchor.sep24.Sep24DepositInfoGenerator;
import org.stellar.anchor.sep24.Sep24Transaction;

public class Sep24DepositInfoSelfGenerator implements Sep24DepositInfoGenerator {

  @Override
  public SepDepositInfo generate(Sep24Transaction txn) {
    String memo = StringUtils.truncate(txn.getId(), 32);
    memo = StringUtils.leftPad(memo, 32, '0');
    memo = new String(Base64.getEncoder().encode(memo.getBytes()));
    return new SepDepositInfo(txn.getToAccount(), memo, memoTypeAsString(MEMO_HASH));
  }
}
