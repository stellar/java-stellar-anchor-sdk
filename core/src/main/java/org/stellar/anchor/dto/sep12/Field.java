package org.stellar.anchor.dto.sep12;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Refer to SEP-12.
 *
 * <p>https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0012.md#fields
 */
@Data
public class Field {
  Type type;
  String description;
  List<String> choices;
  Boolean optional;

  public enum Type {
    @SerializedName("string")
    STRING("string"),

    @SerializedName("binary")
    BINARY("binary"),

    @SerializedName("number")
    NUMBER("number"),

    @SerializedName("date")
    DATE("date");

    private final String name;

    Type(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public static Optional<Type> byName(String match) {
      return Arrays.stream(values()).filter(it -> it.name.equalsIgnoreCase(match)).findFirst();
    }
  }
}
