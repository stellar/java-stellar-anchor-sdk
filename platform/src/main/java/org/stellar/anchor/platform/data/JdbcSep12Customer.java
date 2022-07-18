package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import com.vladmihalcea.hibernate.type.json.JsonType;
import java.util.Map;
import javax.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.stellar.anchor.api.sep.sep12.Field;
import org.stellar.anchor.api.sep.sep12.ProvidedField;
import org.stellar.anchor.api.sep.sep12.Sep12Status;
import org.stellar.anchor.sep12.Sep12Customer;

@Data
@Entity
@Table(name = "sep12_customer")
@TypeDef(name = "json", typeClass = JsonType.class)
public class JdbcSep12Customer implements Sep12Customer {
  @Id String id;

  String account;

  String memo;

  @SerializedName("memo_type")
  String memoType;

  String type;

  String lang;

  Sep12Status status;

  String message;

  @Column(columnDefinition = "json")
  @Type(type = "json")
  Map<String, Field> fields;

  @Column(columnDefinition = "json")
  @Type(type = "json")
  Map<String, ProvidedField> providedFields;
}
