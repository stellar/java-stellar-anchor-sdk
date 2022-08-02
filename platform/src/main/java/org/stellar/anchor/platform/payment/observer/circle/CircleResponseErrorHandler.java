package org.stellar.anchor.platform.payment.observer.circle;

import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;
import org.stellar.anchor.api.exception.HttpException;
import org.stellar.anchor.platform.payment.observer.circle.model.response.CircleError;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClientResponse;

public interface CircleResponseErrorHandler extends CircleGsonParsable {
  @NotNull
  default BiFunction<HttpClientResponse, ByteBufMono, Mono<String>> handleResponseSingle() {
    return (response, bodyBytesMono) -> {
      if (response.status().code() >= 400) {
        return bodyBytesMono
            .asString()
            .map(
                body -> {
                  CircleError circleError = gson.fromJson(body, CircleError.class);
                  throw new HttpException(
                      response.status().code(),
                      circleError.getMessage(),
                      circleError.getCode().toString());
                });
      }

      return bodyBytesMono.asString();
    };
  }
}
