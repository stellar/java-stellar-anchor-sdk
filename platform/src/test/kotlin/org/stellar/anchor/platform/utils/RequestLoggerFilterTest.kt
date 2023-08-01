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
    appLoggingConfig = AppLoggingConfig()
    requestLoggerFilter = RequestLoggerFilter(appLoggingConfig)
    request = MockHttpServletRequest()
    response = MockHttpServletResponse()
    filterChain = mockk<FilterChain>(relaxed = true)
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
    appLoggingConfig.isRequestLoggerEnabled = false

    // Act
    requestLoggerFilter.doFilterInternal(request, response, filterChain)

    // Assert
    verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    verify(exactly = 0) { Log.debugF(any(), *varargAny { true }) }
    verify(exactly = 0) { Log.trace(any()) }
  }

  @Test
  fun `doFilterInternal when RequestLogger enabled should send debug and trace logs`() {
    // Arrange
    appLoggingConfig.isRequestLoggerEnabled = true

    // Act
    requestLoggerFilter.doFilterInternal(request, response, filterChain)

    // Assert
    verify(exactly = 1) { filterChain.doFilter(any(), any()) }
    verify(exactly = 1) { Log.debugF(any(), *varargAny { true }) }
    verify(exactly = 1) { Log.trace(any()) }
  }
}
