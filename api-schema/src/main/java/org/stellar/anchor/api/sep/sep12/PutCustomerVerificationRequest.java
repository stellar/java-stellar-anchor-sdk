package org.stellar.anchor.api.sep.sep12;

import java.util.List;
import lombok.Data;

/**
 * The request body of the PUT /customer/verification endpoint of SEP-12.
 *
 * @see <a
 *     href="https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0012.md#fields">Refer
 *     to SEP-12</a>
 */
@Data
public class PutCustomerVerificationRequest {
  String id;
  List<String> sep9FieldsWithVerification;
}
