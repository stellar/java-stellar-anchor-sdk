package org.stellar.anchor.reference.model;

import com.google.gson.annotations.SerializedName;
import javax.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;

@Data
@Entity
public class Customer {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Type(type = "uuid-char")
  @Column(name = "id", columnDefinition = "VARCHAR(255)", updatable = false, nullable = false)
  String id;

  @SerializedName("stellar_account")
  String stellarAccount;

  @SerializedName("first_name")
  String firstName;

  @SerializedName("last_name")
  String lastName;

  String email;

  @SerializedName("bank_account_number")
  String bankAccountNumber;

  @SerializedName("bank_routing_number")
  String bankRoutingNumber;
}
