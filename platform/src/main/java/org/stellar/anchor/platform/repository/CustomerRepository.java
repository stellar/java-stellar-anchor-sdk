package org.stellar.anchor.platform.repository;

import org.springframework.data.repository.CrudRepository;
import org.stellar.anchor.platform.model.Customer;

public interface CustomerRepository extends CrudRepository<Customer, String> {}
