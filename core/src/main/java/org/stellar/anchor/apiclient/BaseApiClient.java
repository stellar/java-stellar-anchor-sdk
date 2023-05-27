package org.stellar.anchor.apiclient;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.apache.http.HttpStatus;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.exception.SepNotAuthorizedException;
import org.stellar.anchor.api.exception.SepNotFoundException;
import org.stellar.anchor.util.GsonUtils;

public abstract class BaseApiClient {
  static final Gson gson = GsonUtils.getInstance();
  static final OkHttpClient client =
      new OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.MINUTES)
          .readTimeout(10, TimeUnit.MINUTES)
          .writeTimeout(10, TimeUnit.MINUTES)
          .callTimeout(10, TimeUnit.MINUTES)
          .build();

  String handleResponse(Response response) throws AnchorException, IOException {
    if (response.body() == null) throw new SepException("Empty response");

    String responseBody = response.body().string();
    if (response.code() == HttpStatus.SC_FORBIDDEN) {
      throw new SepNotAuthorizedException("Forbidden");
    } else if (response.code() == HttpStatus.SC_NOT_FOUND) {
      throw new SepNotFoundException("Not found");
    } else if (!List.of(HttpStatus.SC_OK, HttpStatus.SC_CREATED, HttpStatus.SC_ACCEPTED)
        .contains(response.code())) {
      throw new SepException(responseBody);
    }

    return responseBody;
  }
}
