package org.stellar.anchor.platform.utils

import io.mockk.*
import javax.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.stellar.anchor.platform.config.AppLoggingConfig
import org.stellar.anchor.util.Log

class RequestLoggerFilterTest {
  private lateinit var appLoggingConfig: AppLoggingConfig
  private lateinit var requestLoggerFilter: RequestLoggerFilter
  private lateinit var request: MockHttpServletRequest
  private lateinit var response: MockHttpServletResponse
  private lateinit var filterChain: FilterChain

  @BeforeEach
  fun setUp() {
    appLoggingConfig = mockk<AppLoggingConfig>(relaxed = true)
    filterChain = mockk<FilterChain>(relaxed = true)
    requestLoggerFilter = RequestLoggerFilter(appLoggingConfig)
    request = MockHttpServletRequest()
    response = MockHttpServletResponse()
    mockkStatic(Log::class)
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun `doFilterInternal when RequestLogger disabled should not send debug and trace logs`() {
    // Arrange
    every { appLoggingConfig.isRequestLoggerEnabled } returns false

    // Act
    requestLoggerFilter.doFilterInternal(request, response, filterChain)

    // Assert
    verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    verify(exactly = 0) { requestLoggerFilter.doFilterWithLogging(any(), any(), any()) }
  }

  @Test
  fun `doFilterInternal when RequestLogger enabled should send debug and trace logs`() {
    // Arrange
    every { appLoggingConfig.isRequestLoggerEnabled } returns false

    // Act
    requestLoggerFilter.doFilterInternal(request, response, filterChain)

    // Assert
    verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    verify(exactly = 1) { requestLoggerFilter.doFilterWithLogging(any(), any(), any()) }
  }
}
