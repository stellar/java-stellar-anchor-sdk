package org.stellar.anchor.platform.data;

import org.jetbrains.annotations.NotNull;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.sep12.Sep12Customer;
import org.stellar.anchor.sep12.Sep12CustomerStore;

public class JdbcSep12CustomerStore implements Sep12CustomerStore {
  private final JdbcSep12CustomerRepo customerRepo;

  public JdbcSep12CustomerStore(JdbcSep12CustomerRepo customerRepo) {
    this.customerRepo = customerRepo;
  }

  @Override
  public Sep12Customer newInstance() {
    return new JdbcSep12Customer();
  }

  @Override
  public Sep12Customer findById(@NotNull String id) {
    return customerRepo.findById(id).orElse(null);
  }

  @Override
  public Sep12Customer save(Sep12Customer sep12Customer) throws SepException {
    if (!(sep12Customer instanceof JdbcSep12Customer)) {
      throw new SepException(
          sep12Customer.getClass() + "  is not a sub-type of " + JdbcSep12Customer.class);
    }
    return customerRepo.save((JdbcSep12Customer) sep12Customer);
  }
}
