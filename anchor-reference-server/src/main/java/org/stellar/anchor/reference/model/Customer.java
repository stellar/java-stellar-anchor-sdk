package org.stellar.anchor.reference.model;

import javax.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Customer")
public class Customer {
  @Id String id;

  // TODO
  // Customers can have many Stellar accounts
  // Our current data model assumes 1 account per customer
  String stellarAccount;

  String memo;

  String memoType;

  String firstName;

  String lastName;

  String email;

  String bankAccountNumber;

  String bankAccountType;

  String bankRoutingNumber;

  String clabeNumber;

  public enum Type {
    SEP31_SENDER("sep31-sender"),
    SEP31_RECEIVER("sep31-receiver");

    private final String name;

    Type(String s) {
      name = s;
    }

    public String toString() {
      return name;
    }
  }

  public enum Status {
    NEEDS_INFO("NEEDS_INFO"),
    ACCEPTED("ACCEPTED"),
    PROCESSING("PROCESSING"),
    ERROR("ERROR");

    private final String name;

    Status(String s) {
      name = s;
    }

    public String toString() {
      return name;
    }
  }
}
