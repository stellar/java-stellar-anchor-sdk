package org.stellar.anchor.client.utils

import io.mockk.*
import javax.servlet.FilterChain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.stellar.anchor.platform.config.AppLoggingConfig

class RequestLoggerFilterTest {
  private lateinit var mockConfig:
    _root_ide_package_.org.stellar.anchor.platform.config.AppLoggingConfig
  private lateinit var request: MockHttpServletRequest
  private lateinit var response: MockHttpServletResponse
  private lateinit var filterChain: FilterChain

  @BeforeEach
  fun setUp() {
    mockConfig =
      mockk<_root_ide_package_.org.stellar.anchor.platform.config.AppLoggingConfig>(relaxed = true)
    request = MockHttpServletRequest()
    response = MockHttpServletResponse()
    filterChain = mockk<FilterChain>(relaxed = true)
  }

  @Test
  fun `test when disabled the doFilterWithLogging() is not called`() {
    // arrange
    every { mockConfig.isRequestLoggerEnabled } returns false
    val a =
      spyk(_root_ide_package_.org.stellar.anchor.platform.utils.RequestLoggerFilter(mockConfig))

    // act
    a.doFilterInternal(request, response, filterChain)

    // assert
    verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    verify(exactly = 0) { a.doFilterWithLogging(any(), any(), any()) }
  }

  @Test
  fun `test when enabled the doFilterWithLogging() is called`() {
    // arrange
    every { mockConfig.isRequestLoggerEnabled } returns true
    val a =
      spyk(_root_ide_package_.org.stellar.anchor.platform.utils.RequestLoggerFilter(mockConfig))

    // act
    a.doFilterInternal(request, response, filterChain)

    // assert
    verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    verify(exactly = 1) { a.doFilterWithLogging(any(), any(), any()) }
  }
}
