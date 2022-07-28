package org.stellar.anchor.util;

import lombok.Data;

@Data
public class AuthHeader<K, V> {
  private K name;
  private V value;

  public AuthHeader(K name, V value) {
    this.name = name;
    this.value = value;
  }
}
