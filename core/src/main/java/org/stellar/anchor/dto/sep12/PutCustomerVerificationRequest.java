package org.stellar.anchor.dto.sep12;

import lombok.Data;

import java.util.List;

@Data
public class PutCustomerVerificationRequest {
  String id;
  List<String> sep9FieldsWithVerficication;
}
