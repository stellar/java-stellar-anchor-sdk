package org.stellar.anchor.platform.utils;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.platform.TransactionsOrderBy;

public class StringEnumConverterFactory {

  public <T> Converter<String, T> converterFor(Function<String, T> valueOf) {
    return new Converter<String, T>() {
      @SneakyThrows
      @Override
      public T convert(@NotNull String source) {
        try {
          return valueOf.apply(source.toUpperCase());
        } catch (IllegalArgumentException e) {
          throw new BadRequestException(
              "Invalid order_by parameter. Possible values are: "
                  + Arrays.stream(TransactionsOrderBy.values())
                      .map(x -> x.name().toLowerCase())
                      .collect(Collectors.toList()));
        }
      }
    };
  }
}
