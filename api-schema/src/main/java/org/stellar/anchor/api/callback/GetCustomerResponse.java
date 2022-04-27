package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.api.shared.Field;
import org.stellar.anchor.api.shared.ProvidedField;

@Data
public class GetCustomerResponse {
  String id;
  String status;
  Map<String, Field> fields;

  @SerializedName("provided_fields")
  Map<String, ProvidedField> providedFields;

  String message;
}
