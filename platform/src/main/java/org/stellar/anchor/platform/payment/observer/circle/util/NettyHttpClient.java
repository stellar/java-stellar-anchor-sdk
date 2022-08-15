package org.stellar.anchor.platform.payment.observer.circle.util;

import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import okhttp3.HttpUrl;
import reactor.netty.http.client.HttpClient;

public class NettyHttpClient {
  public static HttpClient withBaseUrl(String baseUrl) {
    return HttpClient.create()
        .baseUrl(baseUrl)
        .compress(true)
        .followRedirect(false)
        .headers(h -> h.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON))
        .option(
            ChannelOption.CONNECT_TIMEOUT_MILLIS,
            5000) // period within which a connection between a client and a server must be
        // established
        .responseTimeout(
            Duration.ofSeconds(
                30)) // the time we wait to receive a response after sending a request
        .doOnConnected(
            connection ->
                connection
                    .addHandlerLast(new ReadTimeoutHandler(15))
                    .addHandlerLast(new WriteTimeoutHandler(15)));
  }

  public static String buildUri(String uri, LinkedHashMap<String, String> queryParams) {
    HttpUrl.Builder urlBuilder = new HttpUrl.Builder().scheme("https").host("example.com");

    if (uri != null && !uri.isEmpty()) {
      for (String pathSegment : uri.split("/")) {
        urlBuilder = urlBuilder.addPathSegment(pathSegment);
      }
    }

    if (queryParams != null && !queryParams.isEmpty()) {
      for (Map.Entry<String, String> queryParam : queryParams.entrySet()) {
        urlBuilder = urlBuilder.addQueryParameter(queryParam.getKey(), queryParam.getValue());
      }
    }

    return urlBuilder.build().url().getFile();
  }
}
