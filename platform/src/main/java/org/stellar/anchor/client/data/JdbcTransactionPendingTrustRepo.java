package org.stellar.anchor.client.data;

import org.springframework.data.repository.CrudRepository;

public interface JdbcTransactionPendingTrustRepo
    extends CrudRepository<JdbcTransactionPendingTrust, String> {}
