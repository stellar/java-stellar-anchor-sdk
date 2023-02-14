package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.Data;

@Data
public class PutCustomerRequest {
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
  Instant birthDate;

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
  Instant idIssueDate;

  @SerializedName("id_expiration_date")
  Instant idExpirationDate;

  @SerializedName("id_number")
  String idNumber;

  @SerializedName("ip_address")
  String ip_address;

  String sex;
}
