package org.stellar.anchor.apiclient;

public enum TransactionsOrderBy {
  CREATED_AT("started_at"),
  TRANSFER_RECEIVED_AT("transfer_received_at");

  private final String tableName;

  TransactionsOrderBy(String tableName) {
    this.tableName = tableName;
  }

  public String getTableName() {
    return tableName;
  }
}
