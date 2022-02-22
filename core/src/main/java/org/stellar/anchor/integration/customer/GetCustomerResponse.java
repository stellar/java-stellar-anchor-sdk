package org.stellar.anchor.integration.customer;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.stellar.anchor.dto.sep12.Sep12Status;

import java.util.Map;

@Data
public class GetCustomerResponse {
  String id;
  Sep12Status status;

  Map<String, String> fields;

  @SerializedName("provided_fields")
  Map<String, String> providedFields;

  String message;
}
