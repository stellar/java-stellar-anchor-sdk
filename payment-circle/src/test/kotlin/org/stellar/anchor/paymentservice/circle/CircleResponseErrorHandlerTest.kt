package org.stellar.anchor.paymentservice.circle

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.exception.HttpException
import org.stellar.anchor.paymentservice.circle.model.CircleTransfer
import reactor.core.publisher.Mono
import reactor.netty.ByteBufMono
import reactor.netty.http.client.HttpClientResponse

class CircleResponseErrorHandlerTest {
  internal class CircleResponseErrorHandlerImpl : CircleResponseErrorHandler {}

  private lateinit var gson: Gson

  @BeforeEach
  @Throws(IOException::class)
  fun setUp() {
    gson =
      GsonBuilder()
        .registerTypeAdapter(CircleTransfer::class.java, CircleTransfer.Serialization())
        .create()
  }

  @Test
  fun test_private_handleCircleError() {
    // mock objects
    val response = mockk<HttpClientResponse>()
    every { response.status() } returns HttpResponseStatus.BAD_REQUEST

    val bodyBytesMono = mockk<ByteBufMono>()
    every { bodyBytesMono.asString() } returns
      Mono.just("{\"code\":2,\"message\":\"Request body contains unprocessable entity.\"}")

    // run and test
    val impl = CircleResponseErrorHandlerImpl()
    val ex =
      assertThrows<HttpException> {
        impl.handleResponseSingle().apply(response, bodyBytesMono).block()
      }
    verify { response.status() }
    verify { bodyBytesMono.asString() }
    assertEquals(HttpException(400, "Request body contains unprocessable entity.", "2"), ex)
  }
}
