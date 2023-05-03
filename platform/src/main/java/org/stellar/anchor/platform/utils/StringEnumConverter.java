package org.stellar.anchor.platform.utils;

import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.platform.TransactionsOrderBy;
import org.stellar.anchor.api.platform.TransactionsSeps;

// Abstract class because https://github.com/spring-projects/spring-boot/pull/22885
public abstract class StringEnumConverter<T> implements Converter<String, T> {
  abstract T valueOf(String source);

  @SneakyThrows
  @Override
  public T convert(@NotNull String source) {
    try {
      return valueOf(source.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          "Invalid order_by parameter. Possible values are: "
              + Arrays.stream(TransactionsOrderBy.values())
                  .map(x -> x.name().toLowerCase())
                  .collect(Collectors.toList()));
    }
  }

  public static class TransactionsOrderByConverter
      extends StringEnumConverter<TransactionsOrderBy> {
    @Override
    TransactionsOrderBy valueOf(String source) {
      return TransactionsOrderBy.valueOf(source);
    }
  }

  public static class TransactionsSepsConverter extends StringEnumConverter<TransactionsSeps> {
    @Override
    TransactionsSeps valueOf(String source) {
      return TransactionsSeps.valueOf(source);
    }
  }
}
