package org.stellar.anchor.sep12;

import java.util.HashMap;
import java.util.Map;
import org.stellar.anchor.api.sep.sep12.Field;
import org.stellar.anchor.api.sep.sep12.ProvidedField;
import org.stellar.anchor.api.sep.sep12.Sep12Status;

public class Sep12CustomerBuilder {
  private final Sep12Customer customer;

  public Sep12CustomerBuilder(Sep12CustomerStore customerStore) {
    customer = customerStore.newInstance();
  }

  public Sep12CustomerBuilder id(String id) {
    customer.setId(id);
    return this;
  }

  public Sep12CustomerBuilder account(String account) {
    customer.setAccount(account);
    return this;
  }

  public Sep12CustomerBuilder memo(String memo) {
    customer.setMemo(memo);
    return this;
  }

  public Sep12CustomerBuilder memoType(String memoType) {
    customer.setMemoType(memoType);
    return this;
  }

  public Sep12CustomerBuilder type(String type) {
    customer.setType(type);
    return this;
  }

  public Sep12CustomerBuilder lang(String lang) {
    customer.setLang(lang);
    return this;
  }

  public Sep12CustomerBuilder status(Sep12Status status) {
    customer.setStatus(status);
    return this;
  }

  public Sep12CustomerBuilder message(String message) {
    customer.setMessage(message);
    return this;
  }

  public Sep12CustomerBuilder fields(Map<String, Field> fields) {
    customer.setFields(fields);
    return this;
  }

  public Sep12CustomerBuilder fields(String name, Field customerField) {
    HashMap<String, Field> fields =
        customer.getFields() == null ? new HashMap<>() : new HashMap<>(customer.getFields());
    fields.put(name, customerField);
    customer.setFields(fields);
    return this;
  }

  public Sep12CustomerBuilder providedFields(Map<String, ProvidedField> providedFields) {
    customer.setProvidedFields(providedFields);
    return this;
  }

  public Sep12CustomerBuilder providedFields(String name, ProvidedField providedFields) {
    HashMap<String, ProvidedField> fields =
        customer.getProvidedFields() == null
            ? new HashMap<>()
            : new HashMap<>(customer.getProvidedFields());
    fields.put(name, providedFields);
    customer.setProvidedFields(fields);
    return this;
  }

  public Sep12Customer build() {
    return customer;
  }
}
