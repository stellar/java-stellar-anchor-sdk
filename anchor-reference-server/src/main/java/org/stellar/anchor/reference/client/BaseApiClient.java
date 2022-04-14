package org.stellar.anchor.reference.client;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.stellar.anchor.exception.AnchorException;
import org.stellar.anchor.exception.SepException;
import org.stellar.anchor.exception.SepNotAuthorizedException;
import org.stellar.anchor.util.GsonUtils;

public abstract class BaseApiClient {
  static Gson gson = GsonUtils.getInstance();
  static OkHttpClient client =
      new OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.MINUTES)
          .readTimeout(10, TimeUnit.MINUTES)
          .writeTimeout(10, TimeUnit.MINUTES)
          .callTimeout(10, TimeUnit.MINUTES)
          .build();

  String handleResponse(Response response) throws AnchorException, IOException {
    String responseBody = response.body().string();

    if (response.code() == HttpStatus.FORBIDDEN.value()) {
      throw new SepNotAuthorizedException("Forbidden");
    } else if (!List.of(
            HttpStatus.OK.value(), HttpStatus.CREATED.value(), HttpStatus.ACCEPTED.value())
        .contains(response.code())) {
      throw new SepException(responseBody);
    }

    return responseBody;
  }
}
