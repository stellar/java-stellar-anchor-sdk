package org.stellar.anchor.platform.service;

import static org.stellar.anchor.util.MemoHelper.memoTypeAsString;
import static org.stellar.sdk.xdr.MemoType.MEMO_HASH;

import java.util.Base64;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.shared.SepDepositInfo;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.sep6.Sep6DepositInfoGenerator;
import org.stellar.anchor.sep6.Sep6Transaction;

@RequiredArgsConstructor
public class Sep6DepositInfoSelfGenerator implements Sep6DepositInfoGenerator {
  @NonNull private final AssetService assetService;

  @Override
  public SepDepositInfo generate(Sep6Transaction txn) throws AnchorException {
    AssetInfo assetInfo =
        assetService.getAsset(txn.getRequestAssetCode(), txn.getRequestAssetIssuer());

    String memo = StringUtils.truncate(txn.getId(), 32);
    memo = StringUtils.leftPad(memo, 32, '0');
    memo = new String(Base64.getEncoder().encode(memo.getBytes()));
    return new SepDepositInfo(
        assetInfo.getDistributionAccount(), memo, memoTypeAsString(MEMO_HASH));
  }
}
