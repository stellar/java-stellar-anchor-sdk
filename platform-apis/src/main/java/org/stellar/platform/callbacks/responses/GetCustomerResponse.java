package org.stellar.platform.callbacks.responses;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class GetCustomerResponse {
  String id;
  String status;
  Fields fields;

  @SerializedName("provided_fields")
  ProvidedFields providedFields;

  String message;
}

@Data
class Fields {
  String type;
  String description;
  List<String> choices;
  Boolean optional;
}

@Data
class ProvidedFields {
  String type;
  String description;
  List<String> choices;
  Boolean optional;
  String status;
  String error;
}
