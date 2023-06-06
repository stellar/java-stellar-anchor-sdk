package org.stellar.anchor.platform.utils

import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.stellar.anchor.platform.config.AppLoggingConfig
import org.stellar.anchor.util.Log

class RequestLoggerFilterTest {
  private lateinit var appLoggingConfig: AppLoggingConfig
  private lateinit var requestLoggerFilter: RequestLoggerFilter
  private lateinit var request: HttpServletRequest
  private lateinit var response: HttpServletResponse
  private lateinit var filterChain: FilterChain

  @BeforeEach
  fun setUp() {
    appLoggingConfig = AppLoggingConfig()
    requestLoggerFilter = RequestLoggerFilter(appLoggingConfig)
    request = spyk<MockHttpServletRequest>()
    response = spyk<MockHttpServletResponse>()
    filterChain = mockk<FilterChain>(relaxed = true)
    mockkStatic(Log::class)
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
    verify(exactly = 0) { request.method }
    verify(exactly = 0) { request.requestURI }
    verify(exactly = 0) { request.requestURL }
    verify(exactly = 0) { request.queryString }
    verify(exactly = 0) { request.authType }
    verify(exactly = 0) { request.remoteAddr }
    verify(exactly = 0) { request.userPrincipal }
    verify(exactly = 0) { request.getHeader(any()) }
    verify(exactly = 0) { request.cookies }
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
