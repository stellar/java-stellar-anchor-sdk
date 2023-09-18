package org.stellar.anchor.platform.data;

import org.springframework.data.repository.CrudRepository;

public interface JdbcTransactionPendingTrustRepo
    extends CrudRepository<JdbcTransactionPendingTrust, String> {}
