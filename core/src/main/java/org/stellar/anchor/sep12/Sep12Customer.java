package org.stellar.anchor.sep12;

import java.util.Map;
import org.stellar.anchor.api.sep.sep12.Field;
import org.stellar.anchor.api.sep.sep12.ProvidedField;
import org.stellar.anchor.api.sep.sep12.Sep12Status;

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

  String getType();

  void setType(String type);

  String getLang();

  void setLang(String lang);

  Sep12Status getStatus();

  void setStatus(Sep12Status status);

  String getMessage();

  void setMessage(String message);

  Map<String, Field> getFields();

  void setFields(Map<String, Field> fields);

  Map<String, ProvidedField> getProvidedFields();

  void setProvidedFields(Map<String, ProvidedField> providedFields);
}
