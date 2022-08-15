package org.stellar.anchor.api.converter;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Converter<Input, Output> {

  private final Function<Input, Output> convert;
  private final Function<Output, Input> inverse;

  public Converter(final Function<Input, Output> convert, final Function<Output, Input> inverse) {
    this.convert = convert;
    this.inverse = inverse;
  }

  public Output convert(final Input from) {
    return convert.apply(from);
  }

  public Input inverse(final Output to) {
    return inverse.apply(to);
  }

  public final List<Output> convert(final Collection<Input> fromObjects) {
    return fromObjects.stream().map(this::convert).collect(Collectors.toList());
  }

  public final List<Input> inverse(final Collection<Output> toObjects) {
    return toObjects.stream().map(this::inverse).collect(Collectors.toList());
  }
}
