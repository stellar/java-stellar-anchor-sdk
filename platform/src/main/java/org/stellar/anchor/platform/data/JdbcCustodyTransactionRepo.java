package org.stellar.anchor.platform.data;

import org.springframework.data.repository.CrudRepository;

public interface JdbcCustodyTransactionRepo
    extends CrudRepository<JdbcCustodyTransaction, String> {}
