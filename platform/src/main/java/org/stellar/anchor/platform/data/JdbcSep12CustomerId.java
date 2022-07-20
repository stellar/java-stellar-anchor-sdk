package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import javax.persistence.*;
import lombok.Data;
import org.stellar.anchor.sep12.Sep12CustomerId;

@Data
@Entity
@Table(name = "sep12_customer_id")
public class JdbcSep12CustomerId implements Sep12CustomerId {
  @Id String id;

  String account;

  String memo;

  @SerializedName("memo_type")
  String memoType;
}
