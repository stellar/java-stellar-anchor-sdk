package org.stellar.anchor.paymentservice.circle.util;

import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

public class NettyHttpClient {
    public static HttpClient withBaseUrl(String baseUrl) {
        return HttpClient.create()
                .baseUrl(baseUrl)
                .compress(true)
                .followRedirect(false)
                .headers(h -> h.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // period within which a connection between a client and a server must be established
                .responseTimeout(Duration.ofSeconds(30)) // the time we wait to receive a response after sending a request
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(15))
                        .addHandlerLast(new WriteTimeoutHandler(15)));
    }
}
