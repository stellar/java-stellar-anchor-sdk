package org.stellar.anchor.platform.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class CustomerStatus {
  @Id private String type;
  @ManyToOne private Customer customer;
  private String status;
}
