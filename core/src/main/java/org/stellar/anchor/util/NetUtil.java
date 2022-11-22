package org.stellar.anchor.util;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

public class NetUtil {
  public static String fetch(String url) throws IOException {
    Request request = OkHttpUtil.buildGetRequest(url);
    Response response = getCall(request).execute();

    if (response.body() == null) return "";
    return Objects.requireNonNull(response.body()).string();
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public static boolean isUrlValid(String url) {
    /* Try creating a valid URL */
    try {
      new URL(url).toURI();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  static Call getCall(Request request) {
    return OkHttpUtil.buildClient().newCall(request);
  }
}
