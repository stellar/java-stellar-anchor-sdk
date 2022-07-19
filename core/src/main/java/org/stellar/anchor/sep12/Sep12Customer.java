package org.stellar.anchor.sep12;

public interface Sep12Customer {
  String getId();

  void setId(String id);

  @Deprecated
  String getAccount();

  @Deprecated
  void setAccount(String account);

  String getMemo();

  void setMemo(String memo);

  @Deprecated
  String getMemoType();

  @Deprecated
  void setMemoType(String memoType);
}
