package org.stellar.anchor.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class UrlValidationUtil {
  public static UrlConnectionStatus validateUrl(String urlString) {
    try {
      URL url = new URL(urlString);
      URLConnection conn = url.openConnection();
      conn.connect();
    } catch (MalformedURLException e) {
      return UrlConnectionStatus.MALFORMED;
    } catch (IOException e) {
      return UrlConnectionStatus.UNREACHABLE;
    }
    return UrlConnectionStatus.VALID;
  }


  /*boolean checkHealth(String endpoint) throws java.io.IOException {
    Request request =
        new Request.Builder()
            .url(endpoint + "/health")
            .header("Content-Type", "application/json")
            .get()
            .build();

    OkHttpClient client =
        new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .callTimeout(10, TimeUnit.MINUTES)
            .build();
    Response response = client.newCall(request).execute();
    String responseBody = response.body().string();
    return true;*/
}
