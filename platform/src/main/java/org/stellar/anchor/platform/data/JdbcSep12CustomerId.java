package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import com.vladmihalcea.hibernate.type.json.JsonType;
import javax.persistence.*;
import lombok.Data;
import org.hibernate.annotations.TypeDef;
import org.stellar.anchor.sep12.Sep12CustomerId;

@Data
@Entity
@Table(name = "sep12_customer_id")
@TypeDef(name = "json", typeClass = JsonType.class)
public class JdbcSep12CustomerId implements Sep12CustomerId {
  @Id String id;

  String account;

  String memo;

  @SerializedName("memo_type")
  String memoType;
}
