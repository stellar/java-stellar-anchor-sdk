package org.stellar.anchor.api.sep.sep12;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

/**
 * Refer to SEP-12.
 *
 * <p>https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0012.md#request-1
 */
@Data
@Builder
public class Sep12PutCustomerRequest implements Sep12CustomerRequestBase {
  String id;
  String account;
  String memo;

  @SerializedName("memo_type")
  String memoType;

  String type;

  @SerializedName("first_name")
  String firstName;

  @SerializedName("last_name")
  String lastName;

  @SerializedName("additional_name")
  String additionalName;

  @SerializedName("address_country_code")
  String addressCountryCode;

  @SerializedName("state_or_province")
  String stateOrProvince;

  String city;

  @SerializedName("postal_code")
  String postalCode;

  String address;

  @SerializedName("mobile_number")
  String mobileNumber;

  @SerializedName("email_address")
  String emailAddress;

  @SerializedName("birth_date")
  String birthDate;

  @SerializedName("birth_place")
  String birthPlace;

  @SerializedName("birth_country_code")
  String birthCountryCode;

  @SerializedName("bank_account_number")
  String bankAccountNumber;

  @SerializedName("bank_account_type")
  String bankAccountType;

  @SerializedName("bank_number")
  String bankNumber;

  @SerializedName("bank_phone_number")
  String bankPhoneNumber;

  @SerializedName("bank_branch_number")
  String bankBranchNumber;

  @SerializedName("clabe_number")
  String clabeNumber;

  @SerializedName("cbu_number")
  String cbuNumber;

  @SerializedName("cbu_alias")
  String cbuAlias;

  @SerializedName("tax_id")
  String taxId;

  @SerializedName("tax_id_name")
  String taxIdName;

  String occupation;

  @SerializedName("employer_name")
  String employerName;

  @SerializedName("employer_address")
  String employerAddress;

  @SerializedName("language_code")
  String languageCode;

  @SerializedName("id_type")
  String idType;

  @SerializedName("id_country_code")
  String idCountryCode;

  @SerializedName("id_issue_date")
  String idIssueDate;

  @SerializedName("id_expiration_date")
  String idExpirationDate;

  @SerializedName("id_number")
  String idNumber;

  @SerializedName("ip_address")
  String ipAddress;

  String sex;
}
