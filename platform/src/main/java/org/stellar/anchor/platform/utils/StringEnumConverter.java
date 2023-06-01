package org.stellar.anchor.platform.utils;

import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Sort;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.apiclient.TransactionsOrderBy;
import org.stellar.anchor.apiclient.TransactionsSeps;

// Abstract class because https://github.com/spring-projects/spring-boot/pull/22885
public abstract class StringEnumConverter<T extends Enum<T>> implements Converter<String, T> {
  private final Class<T> enumClass;

  protected StringEnumConverter(Class<T> enumClass) {
    this.enumClass = enumClass;
  }

  @SneakyThrows
  @Override
  public T convert(@NotNull String source) {
    Optional<String> serializedName =
        Arrays.stream(enumClass.getDeclaredFields())
            .filter(x -> x.getAnnotation(SerializedName.class) != null)
            .filter(x -> x.getAnnotation(SerializedName.class).value().equals(source))
            .findAny()
            .map(Field::getName);

    Optional<T> value =
        Arrays.stream(enumClass.getEnumConstants())
            .filter(x -> x.name().equalsIgnoreCase(serializedName.orElse(source)))
            .findAny();

    return value.orElseThrow(() -> new BadRequestException("Invalid enum parameter"));
  }

  public static class TransactionsOrderByConverter
      extends StringEnumConverter<TransactionsOrderBy> {
    public TransactionsOrderByConverter() {
      super(TransactionsOrderBy.class);
    }
  }

  public static class TransactionsSepsConverter extends StringEnumConverter<TransactionsSeps> {
    public TransactionsSepsConverter() {
      super(TransactionsSeps.class);
    }
  }

  public static class SepTransactionStatusConverter
      extends StringEnumConverter<SepTransactionStatus> {
    public SepTransactionStatusConverter() {
      super(SepTransactionStatus.class);
    }
  }

  public static class DirectionConverter extends StringEnumConverter<Sort.Direction> {
    public DirectionConverter() {
      super(Sort.Direction.class);
    }
  }
}
