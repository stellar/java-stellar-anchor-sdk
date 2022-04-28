package org.stellar.anchor.util;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/** GSON serializer/deserializer for converting {@link Instant} objects. */
public class InstantConverter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
  private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_INSTANT;

  @Override
  public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
    return new JsonPrimitive(dateTimeFormatter.format(src));
  }

  @Override
  public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    return dateTimeFormatter.parse(json.getAsString(), Instant::from);
  }
}
