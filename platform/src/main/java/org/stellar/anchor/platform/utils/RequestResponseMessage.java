package org.stellar.anchor.platform.utils;

import java.util.stream.Collectors;
import lombok.Builder;

@Builder
public class RequestResponseMessage {
  long durationMilliseconds;
  Request request;
  Response response;

  @Override
  public String toString() {
    return String.format(
        "\"RequestResponseMessage\": {\"durationMilliseconds\": %s, %s, %s}",
        getStrValue(durationMilliseconds), getStrValue(request), getStrValue(response));
  }

  @Builder
  static class Request {
    String method;
    String path;
    String queryParams;
    String authType;
    String principalName;
    String clientId;

    @Override
    public String toString() {
      String fullPath = path;
      if (queryParams != null) {
        fullPath += String.format("?%s", queryParams);
      }
      return String.format(
          "\"Request\": {\"method\": %s, \"fullPath\": %s, \"authType\": %s, \"principalName\": %s, \"clientId\": %s}",
          getStrValue(method),
          getStrValue(fullPath),
          getStrValue(authType),
          getStrValue(principalName),
          getStrValue(clientId));
    }
  }

  @Builder
  static class Response {
    int statusCode;
    String responseBody;

    @Override
    public String toString() {
      String responseBody =
          this.responseBody.lines().map(String::trim).collect(Collectors.joining(" "));
      return String.format(
          "\"Response\": {\"statusCode\": %s, \"responseBody\": %s}",
          getStrValue(statusCode), getStrValue(responseBody));
    }
  }

  static String getStrValue(Object obj) {
    if (obj == null) {
      return null;
    }
    if (obj instanceof String) {
      return String.format("\"%s\"", obj);
    }
    return obj.toString();
  }
}
