package org.stellar.platform.apis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.stellar.platform.apis.events.requests.EventRequest;
import org.stellar.platform.apis.events.requests.EventRequestDeserializer;

public class GsonSingleton {
  private static Gson instance = null;

  protected GsonSingleton() {}

  public static Gson getInstance() {
    if (instance == null) {
      instance = new GsonBuilder().registerTypeAdapter(EventRequest.class, new EventRequestDeserializer()).create();
    }
    return instance;
  }
}
