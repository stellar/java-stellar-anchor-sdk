package org.stellar.anchor.sep9;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sep9Fields {
  public static final List<String> SEP_9_FIELDS =
      List.of(
          "family_name",
          "last_name",
          "given_name",
          "first_name",
          "additional_name",
          "address_country_code",
          "state_or_province",
          "city",
          "postal_code",
          "address",
          "mobile_number",
          "email_address",
          "birth_date",
          "birth_place",
          "birth_country_code",
          "bank_account_number",
          "bank_account_type",
          "bank_number",
          "bank_phone_number",
          "bank_branch_number",
          "cbu_number",
          "cbu_alias",
          "clabe_number",
          "tax_id",
          "tax_id_name",
          "occupation",
          "employer_name",
          "employer_address",
          "language_code",
          "id_type",
          "id_country_code",
          "id_issue_date",
          "id_expiration_date",
          "id_number",
          "photo_id_front",
          "photo_id_back",
          "notary_approval_of_photo_id",
          "ip_address",
          "photo_proof_residence",
          "organization.name",
          "organization.VAT_number",
          "organization.registration_number",
          "organization.registered_address",
          "organization.number_of_shareholders",
          "organization.shareholder_name",
          "organization.photo_incorporation_doc",
          "organization.photo_proof_address",
          "organization.address_country_code",
          "organization.state_or_province",
          "organization.city",
          "organization.postal_code",
          "organization.director_name",
          "organization.website",
          "organization.email",
          "organization.phone");

  public static HashMap<String, String> extractSep9Fields(Map<String, String> wr) {
    HashMap<String, String> sep9Fields = new HashMap<>();
    for (Map.Entry<String, String> entry : wr.entrySet()) {
      if (Sep9Fields.SEP_9_FIELDS.contains(entry.getKey())) {
        sep9Fields.put(entry.getKey(), entry.getValue());
      }
    }
    return sep9Fields;
  }
}
