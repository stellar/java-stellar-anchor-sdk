package org.stellar.anchor.platform.data;

import static org.stellar.anchor.api.sep.SepTransactionStatus.mergeStatusesList;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import java.util.List;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.util.TransactionsParams;

public class AllTransactionsRepositoryImpl<T> implements AllTransactionsRepository<T> {
  private final EntityManager em;
  private static final String NL = System.lineSeparator();

  public AllTransactionsRepositoryImpl(EntityManager em) {
    this.em = em;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<T> findAllTransactions(TransactionsParams params, Class<T> entityClass) {
    JpaEntityInformation<T, ?> entityInformation =
        JpaEntityInformationSupport.getEntityInformation(entityClass, em);
    Table table = entityInformation.getJavaType().getAnnotation(Table.class);

    if (table == null || table.name().isEmpty()) {
      throw new AssertionError("Class " + entityClass.getName() + " doesn't have table name");
    }

    List<SepTransactionStatus> statuses = params.getStatuses();

    // Create query
    String nativeQuery =
        String.format(
            "SELECT * FROM %s t %s ORDER BY %s %s NULLS LAST, id ASC LIMIT %d OFFSET %d",
            table.name(),
            statuses == null ? "" : " WHERE t.status in (" + mergeStatusesList(statuses, "'") + ")",
            params.getOrderBy().getTableName(),
            params.getOrder().name(),
            params.getPageSize(),
            params.getPageNumber() * params.getPageSize());

    Query query = em.createNativeQuery(nativeQuery, entityClass);

    List<T> results = query.getResultList();

    return results;
  }
}
