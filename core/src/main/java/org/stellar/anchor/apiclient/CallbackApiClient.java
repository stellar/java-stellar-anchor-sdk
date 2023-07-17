package org.stellar.anchor.apiclient;

import com.google.gson.Gson;
import java.io.IOException;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.stellar.anchor.api.callback.SendEventRequest;
import org.stellar.anchor.api.callback.SendEventResponse;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.OkHttpUtil;

public class CallbackApiClient extends BaseApiClient {
  static final Gson gson = GsonUtils.getInstance();

  public CallbackApiClient(AuthHelper authHelper, String endpoint) {
    super(authHelper, endpoint);
  }

  public SendEventResponse sendEvent(SendEventRequest sendEventRequest)
      throws AnchorException, IOException {
    HttpUrl url = HttpUrl.parse(endpoint);
    if (url == null)
      throw new InvalidConfigException(
          String.format("Invalid endpoint: %s of the client.", endpoint));
    url =
        new HttpUrl.Builder()
            .scheme(url.scheme())
            .host(url.host())
            .port(url.port())
            .addPathSegment("events")
            .build();

    RequestBody requestBody = OkHttpUtil.buildJsonRequestBody(gson.toJson(sendEventRequest));
    Request request = getRequestBuilder().url(url).post(requestBody).build();
    Response response = client.newCall(request).execute();
    return gson.fromJson(handleResponse(response), SendEventResponse.class);
  }
}
