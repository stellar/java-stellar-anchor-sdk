package org.stellar.anchor.platform.utils;

import lombok.SneakyThrows;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.platform.GetTransactionResponse;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.sep31.Sep31Transaction;
import org.stellar.anchor.util.TransactionHelper;

public class PlatformTransactionHelper {
  @SneakyThrows
  public static GetTransactionResponse toGetTransactionResponse(
      JdbcSepTransaction txn, AssetService assetService) {
    switch (txn.getProtocol()) {
      case "24":
        return TransactionHelper.toGetTransactionResponse((Sep24Transaction) txn, assetService);
      case "31":
        return TransactionHelper.toGetTransactionResponse((Sep31Transaction) txn);
      default:
        throw new SepException(String.format("Unsupported protocol:%s", txn.getProtocol()));
    }
  }
}
