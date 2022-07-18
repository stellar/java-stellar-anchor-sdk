package org.stellar.anchor.sep12;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.api.sep.sep12.Field;
import org.stellar.anchor.api.sep.sep12.ProvidedField;
import org.stellar.anchor.api.sep.sep12.Sep12Status;

@Data
public class PojoSep12Customer implements Sep12Customer {
  String id;

  String account;

  String memo;

  @SerializedName("memo_type")
  String memoType;

  String type;

  String lang;

  Sep12Status status;

  String message;

  Map<String, Field> fields;

  Map<String, ProvidedField> providedFields;
}
