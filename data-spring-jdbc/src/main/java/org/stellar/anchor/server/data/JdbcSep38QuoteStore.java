package org.stellar.anchor.server.data;

import org.jetbrains.annotations.NotNull;
import org.stellar.anchor.exception.NotFoundException;
import org.stellar.anchor.exception.SepException;
import org.stellar.anchor.model.Sep38Quote;
import org.stellar.anchor.sep38.Sep38QuoteStore;

public class JdbcSep38QuoteStore implements Sep38QuoteStore {
  static final String CB_KEY_NAMESPACE = "SAS:RESOURCE:";
  final JdbcSep38QuoteRepo txnRepo;

  public JdbcSep38QuoteStore(JdbcSep38QuoteRepo txnRepo) {
    this.txnRepo = txnRepo;
  }

  @Override
  public Sep38Quote newInstance() {
    return new JdbcSep38Quote();
  }

  @Override
  public Sep38Quote findByQuoteId(@NotNull String quoteId) throws NotFoundException {
    return txnRepo
        .findById(quoteId)
        .orElseThrow(
            () ->
                new NotFoundException(String.format("customer for 'id' '%s' not found", quoteId)));
  }

  @Override
  public Sep38Quote save(Sep38Quote sep38Quote) throws SepException {
    if (!(sep38Quote instanceof JdbcSep38Quote)) {
      throw new SepException(
          sep38Quote.getClass() + "  is not a sub-type of " + JdbcSep38Quote.class);
    }
    JdbcSep38Quote quote = (JdbcSep38Quote) sep38Quote;
    return txnRepo.save(quote);
  }
}
