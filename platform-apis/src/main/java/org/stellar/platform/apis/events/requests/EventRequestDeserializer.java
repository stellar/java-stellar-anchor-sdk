package org.stellar.platform.apis.events.requests;

import com.google.gson.*;
import java.lang.reflect.Type;
import org.stellar.platform.apis.shared.Quote;
import org.stellar.platform.apis.shared.Transaction;

public class EventRequestDeserializer implements JsonDeserializer<EventRequest> {
  @Override
  public EventRequest deserialize(
      JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    if (!json.isJsonObject()) {
      throw new JsonParseException("EventRequest messages must be JSON objects");
    }
    EventRequest eventRequest = new EventRequest();
    JsonObject eventRequestJsonObject = json.getAsJsonObject();
    eventRequest.setId(eventRequestJsonObject.get("id").getAsString());
    eventRequest.setType(eventRequestJsonObject.get("type").getAsString());
    JsonObject dataJsonObject = eventRequestJsonObject.get("data").getAsJsonObject();
    if (eventRequest.getType().contains("transaction")) {
      eventRequest.setData(context.deserialize(dataJsonObject, Transaction.class));
    } else if (eventRequest.getType().contains("quote")) {
      eventRequest.setData(context.deserialize(dataJsonObject, Quote.class));
    } else {
      throw new JsonParseException("EventRequest.type not recognized");
    }
    return eventRequest;
  }
}
