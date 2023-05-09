package org.stellar.anchor.api.platform;

public enum TransactionsOrderBy {
  CREATED_AT("startedAt"),
  TRANSFER_RECEIVED_AT("transferReceivedAt");

  private final String tableName;

  TransactionsOrderBy(String tableName) {
    this.tableName = tableName;
  }

  public String getTableName() {
    return tableName;
  }
}
