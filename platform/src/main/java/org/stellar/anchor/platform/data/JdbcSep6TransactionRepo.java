package org.stellar.anchor.platform.data;

import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.stellar.anchor.sep6.Sep6Transaction;

public interface JdbcSep6TransactionRepo
    extends PagingAndSortingRepository<JdbcSep6Transaction, String>,
        AllTransactionsRepository<JdbcSep6Transaction> {

  @NotNull
  Optional<JdbcSep6Transaction> findById(@NonNull String id);

  Sep6Transaction findOneByTransactionId(String transactionId);

  Sep6Transaction findOneByStellarTransactionId(String stellarTransactionId);

  Sep6Transaction findOneByExternalTransactionId(String externalTransactionId);

  List<Sep6Transaction> findBySep10AccountAndRequestAssetCodeOrderByStartedAtDesc(
      String stellarAccount, String assetCode);
}
