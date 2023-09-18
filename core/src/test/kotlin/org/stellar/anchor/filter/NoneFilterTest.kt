package org.stellar.anchor.filter

import io.mockk.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.junit.jupiter.api.*
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
internal class NoneFilterTest {
  private lateinit var noneFilter: NoneFilter
  private lateinit var request: HttpServletRequest
  private lateinit var response: HttpServletResponse
  private lateinit var mockFilterChain: FilterChain

  @BeforeEach
  fun setup() {
    this.noneFilter = NoneFilter()
    this.request = mockk(relaxed = true)
    this.response = mockk(relaxed = true)
    this.mockFilterChain = mockk(relaxed = true)
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
  }

  @ParameterizedTest
  @ValueSource(strings = ["GET", "PUT", "POST", "DELETE", "OPTIONS"])
  fun `test NoneFilter works without Authorization header`(method: String) {
    every { request.method } returns method
    every { request.getHeader("Authorization") } returns null

    noneFilter.doFilter(request, response, mockFilterChain)

    verify { mockFilterChain.doFilter(request, response) }
  }
}
