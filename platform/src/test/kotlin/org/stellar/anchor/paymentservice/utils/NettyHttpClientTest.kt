package org.stellar.anchor.paymentservice.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.stellar.anchor.platform.payment.observer.circle.util.NettyHttpClient

class NettyHttpClientTest {
  @Test
  fun testBuildUri() {
    assertEquals("/", NettyHttpClient.buildUri(null, null))
    assertEquals("/foo", NettyHttpClient.buildUri("/foo", null))
    assertEquals("/foo", NettyHttpClient.buildUri("foo", null))
    assertEquals("/foo/bar", NettyHttpClient.buildUri("foo/bar", null))
    assertEquals("/foo/bar", NettyHttpClient.buildUri("/foo/bar", null))

    assertEquals("/?key1=val1", NettyHttpClient.buildUri(null, linkedMapOf("key1" to "val1")))
    assertEquals("/foo?key1=val1", NettyHttpClient.buildUri("/foo", linkedMapOf("key1" to "val1")))
    var queryParams = linkedMapOf("key1" to "val1", "key2" to "val2")
    assertEquals("/foo?key1=val1&key2=val2", NettyHttpClient.buildUri("/foo", queryParams))
    queryParams = linkedMapOf("key2" to "val2", "key1" to "val1")
    assertEquals("/foo?key2=val2&key1=val1", NettyHttpClient.buildUri("/foo", queryParams))
  }
}
