package org.stellar.anchor.util

import io.mockk.*
import io.mockk.impl.annotations.MockK
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.util.NetUtil.isUrlValid

internal class NetUtilTest {
  @MockK private lateinit var mockCall: okhttp3.Call

  @MockK private lateinit var mockResponse: Response

  @MockK private lateinit var mockResponseBody: ResponseBody

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxed = true)
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun `test fetch()`() {
    mockkStatic(NetUtil::class)
    every { NetUtil.getCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.body } returns mockResponseBody
    every { mockResponseBody.string() } returns "result"

    val result = NetUtil.fetch("http://hello")
    assert(result.equals("result"))
    verify {
      NetUtil.getCall(any())
      mockCall.execute()
    }
  }

  @Test
  fun `test fetch() throws exception`() {
    mockkStatic(NetUtil::class)
    every { NetUtil.getCall(any()) } returns mockCall
    every { mockCall.execute() } returns mockResponse
    every { mockResponse.body } returns null
    every { mockResponseBody.string() } returns "result"

    val result = NetUtil.fetch("http://hello")
    assert(result.equals(""))
  }

  @Test
  fun `test getCall()`() {
    val request = OkHttpUtil.buildGetRequest("https://www.stellar.org")
    assertNotNull(NetUtil.getCall(request))
  }

  @Test
  fun `test good urls with isUrlValid()`() {
    assertTrue(isUrlValid("http://www.stellar.org"))
    assertTrue(isUrlValid("https://www.stellar.org/"))
    assertTrue(isUrlValid("https://www.stellar.org/.well-known/stellar.toml"))
    assertTrue(isUrlValid("https://www.stellar.org/sep1?q=123&p=false"))
    assertTrue(isUrlValid("https://www.stellar.org/sep1?q=&p=false"))
    assertTrue(isUrlValid("https://www.stellar.org/a/b/c"))
    assertTrue(isUrlValid("https://www.stellar.org/a/"))
    assertTrue(isUrlValid("http://192.168.100.1"))
    assertTrue(isUrlValid("http://192.168.100.1/a/"))
    assertTrue(isUrlValid("ftp://ftp.stellar.org"))
    assertTrue(isUrlValid("ftp://ftp.stellar.org/a/b/c"))
    assertTrue(isUrlValid("ftp://ftp.stellar.org/a/"))
    assertTrue(isUrlValid("file:///home/johndoe/a.toml"))
  }

  @Test
  fun `test bad urls with isUrlValid()`() {
    assertFalse(isUrlValid("http:// www.stellar.org"))
    assertFalse(isUrlValid("https:// www.stellar.org"))
    assertFalse(isUrlValid("https:// www.stellar.org/a /"))
    assertFalse(isUrlValid("https:// www.stellar.org/a?p=123&q= false"))
    assertFalse(isUrlValid("https://192.168.100 .1"))
    assertFalse(isUrlValid("abc://www.stellar.org"))
    assertFalse(isUrlValid("http:// www.stellar.org"))
  }
}
