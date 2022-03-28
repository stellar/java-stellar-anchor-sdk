package org.stellar.anchor.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.Instant;

public class GsonUtils {
  private static Gson instance = null;

  public static GsonBuilder builder() {
    return new GsonBuilder().registerTypeAdapter(Instant.class, new InstantConverter());
  }

  public static Gson getGsonInstance() {
    if (instance == null) instance = builder().create();
    return instance;
  }
}
