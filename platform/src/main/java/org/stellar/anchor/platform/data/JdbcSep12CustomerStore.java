package org.stellar.anchor.platform.data;

import org.jetbrains.annotations.NotNull;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.sep12.Sep12CustomerId;
import org.stellar.anchor.sep12.Sep12CustomerStore;

public class JdbcSep12CustomerStore implements Sep12CustomerStore {
  private final JdbcSep12CustomerRepo customerRepo;

  public JdbcSep12CustomerStore(JdbcSep12CustomerRepo customerRepo) {
    this.customerRepo = customerRepo;
  }

  @Override
  public Sep12CustomerId newInstance() {
    return new JdbcSep12CustomerId();
  }

  @Override
  public Sep12CustomerId findById(@NotNull String id) {
    return customerRepo.findById(id).orElse(null);
  }

  @Override
  public Sep12CustomerId save(Sep12CustomerId sep12CustomerId) throws SepException {
    if (!(sep12CustomerId instanceof JdbcSep12CustomerId)) {
      throw new SepException(
          sep12CustomerId.getClass() + "  is not a sub-type of " + JdbcSep12CustomerId.class);
    }
    return customerRepo.save((JdbcSep12CustomerId) sep12CustomerId);
  }
}
