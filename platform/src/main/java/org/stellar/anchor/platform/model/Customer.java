package org.stellar.anchor.platform.model;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
public class Customer {
  @Id private String id;
  @OneToMany private Set<CustomerStatus> statuses;
}
