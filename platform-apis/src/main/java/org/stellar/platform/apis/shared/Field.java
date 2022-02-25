package org.stellar.platform.apis.shared;

import lombok.Data;

import java.util.List;

@Data
public class Field {
  String type;
  String description;
  List<String> choices;
  Boolean optional;
}
