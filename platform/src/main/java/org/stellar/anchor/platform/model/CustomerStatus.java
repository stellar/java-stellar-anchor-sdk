package org.stellar.anchor.platform.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;

@Entity
@Data
public class CustomerStatus {
  @Id private String type;
  private String status;
}
