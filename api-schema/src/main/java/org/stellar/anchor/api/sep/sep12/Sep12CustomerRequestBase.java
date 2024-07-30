package org.stellar.anchor.api.sep.sep12;

public interface Sep12CustomerRequestBase {
  String getId();

  void setId(String id);

  String getAccount();

  void setAccount(String account);

  String getMemo();

  void setMemo(String memo);

  String getMemoType();

  void setMemoType(String memoType);

  String getTransactionId();

  void setTransactionId(String transactionId);

  String getType();

  void setType(String type);
}
