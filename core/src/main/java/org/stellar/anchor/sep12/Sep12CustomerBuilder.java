package org.stellar.anchor.sep12;

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

  public Sep12Customer build() {
    return customer;
  }
}
