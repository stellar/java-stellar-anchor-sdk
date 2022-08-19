package org.stellar.anchor.util

import io.mockk.*
import io.mockk.impl.annotations.MockK
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
}
