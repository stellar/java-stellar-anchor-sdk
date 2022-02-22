package org.stellar.anchor.integration.customer;

import lombok.Data;

@Data
public class GetCustomerRequest {
  String id;
  String account;
  String type;
  String memo;
  String memoType;
}
