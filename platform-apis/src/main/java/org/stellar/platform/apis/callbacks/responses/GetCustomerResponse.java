package org.stellar.platform.apis.callbacks.responses;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;
import org.stellar.platform.apis.shared.Field;
import org.stellar.platform.apis.shared.ProvidedField;

@Data
public class GetCustomerResponse {
  String id;
  String status;
  Map<String, Field> fields;

  @SerializedName("provided_fields")
  Map<String, ProvidedField> providedFields;

  String message;
}
