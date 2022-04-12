package org.stellar.anchor.server.data;

import com.google.gson.annotations.SerializedName;
import javax.persistence.*;
import lombok.Data;

@Data
@Entity
@Access(AccessType.FIELD)
@Table(name = "stellar_account_page_token")
public class StellarAccountPageToken {
  @Id
  @SerializedName("account_id")
  String accountId;

  String token;
}
