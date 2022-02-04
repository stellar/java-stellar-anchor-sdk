package org.stellar.anchor.paymentservice.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.stellar.anchor.paymentservice.circle.util.NettyHttpClient

class NettyHttpClientTest {
  @Test
  fun testUriWithParams() {
    assertEquals("/", NettyHttpClient.uriWithParams(null, null))
    assertEquals("/foo", NettyHttpClient.uriWithParams("/foo", null))
    assertEquals("/foo", NettyHttpClient.uriWithParams("foo", null))
    assertEquals("/foo/bar", NettyHttpClient.uriWithParams("foo/bar", null))
    assertEquals("/foo/bar", NettyHttpClient.uriWithParams("/foo/bar", null))

    assertEquals("/?key1=val1", NettyHttpClient.uriWithParams(null, linkedMapOf("key1" to "val1")))
    assertEquals(
      "/foo?key1=val1",
      NettyHttpClient.uriWithParams("/foo", linkedMapOf("key1" to "val1"))
    )
    var queryParams = linkedMapOf("key1" to "val1", "key2" to "val2")
    assertEquals("/foo?key1=val1&key2=val2", NettyHttpClient.uriWithParams("/foo", queryParams))
    queryParams = linkedMapOf("key2" to "val2", "key1" to "val1")
    assertEquals("/foo?key2=val2&key1=val1", NettyHttpClient.uriWithParams("/foo", queryParams))
  }
}
