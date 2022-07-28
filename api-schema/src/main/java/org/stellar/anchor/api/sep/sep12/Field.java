package org.stellar.anchor.api.sep.sep12;

import com.google.gson.annotations.SerializedName;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refer to SEP-12.
 *
 * <p>https://github.com/stellar/stellar-protocol/blob/master/ecosystem/sep-0012.md#fields
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
