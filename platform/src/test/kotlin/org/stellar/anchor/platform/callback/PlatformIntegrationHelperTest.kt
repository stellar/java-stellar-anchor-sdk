package org.stellar.anchor.platform.callback

import io.mockk.*
import java.util.*
import kotlin.test.assertEquals
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.stellar.anchor.auth.*
import org.stellar.anchor.auth.ApiAuthJwt.PlatformAuthJwt
import org.stellar.anchor.auth.AuthType.*
import org.stellar.anchor.auth.JwtService

class PlatformIntegrationHelperTest {
  companion object {
    const val JWT_EXPIRATION_MILLISECONDS: Long = 90000
    const val TEST_HOME_DOMAIN = "http://localhost:8080"
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
  }

  @ParameterizedTest
  @EnumSource(AuthType::class)
  fun test_getRequestBuilder(authType: AuthType) {
    when (authType) {
      JWT -> {
        // Mock calendar to guarantee the jwt token format
        val calendarSingleton = Calendar.getInstance()
        val currentTimeMilliseconds = calendarSingleton.timeInMillis
        mockkObject(calendarSingleton)
        every { calendarSingleton.timeInMillis } returns currentTimeMilliseconds
        every { calendarSingleton.timeInMillis = any() } answers { callOriginal() }
        mockkStatic(Calendar::class)
        every { Calendar.getInstance() } returns calendarSingleton

        // mock jwt token based on the mocked calendar
        val wantJwtToken =
          PlatformAuthJwt(
            currentTimeMilliseconds / 1000L,
            (currentTimeMilliseconds + JWT_EXPIRATION_MILLISECONDS) / 1000L
          )

        val jwtService = JwtService(null, null, null, "secret", "secret")
        val authHelper = AuthHelper.forJwtToken(jwtService, JWT_EXPIRATION_MILLISECONDS)

        val gotRequestBuilder = PlatformIntegrationHelper.getRequestBuilder(authHelper)
        val gotRequest = gotRequestBuilder.url(TEST_HOME_DOMAIN).get().build()
        val wantRequestBuilder: Request.Builder =
          Request.Builder()
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${jwtService.encode(wantJwtToken)}")
        val wantRequest = wantRequestBuilder.url(TEST_HOME_DOMAIN).get().build()
        assertEquals(wantRequest.headers, gotRequest.headers)
      }
      API_KEY -> {
        val authHelper = AuthHelper.forApiKey("secret")
        val gotRequestBuilder = PlatformIntegrationHelper.getRequestBuilder(authHelper)
        val gotRequest = gotRequestBuilder.url(TEST_HOME_DOMAIN).get().build()
        val wantRequestBuilder: Request.Builder =
          Request.Builder().header("Content-Type", "application/json").header("X-Api-Key", "secret")
        val wantRequest = wantRequestBuilder.url(TEST_HOME_DOMAIN).get().build()
        assertEquals(wantRequest.headers, gotRequest.headers)
      }
      NONE -> {
        val authHelper = AuthHelper.forNone()
        val gotRequestBuilder = PlatformIntegrationHelper.getRequestBuilder(authHelper)
        val gotRequest = gotRequestBuilder.url(TEST_HOME_DOMAIN).get().build()
        val wantRequestBuilder: Request.Builder =
          Request.Builder().header("Content-Type", "application/json")
        val wantRequest = wantRequestBuilder.url(TEST_HOME_DOMAIN).get().build()
        assertEquals(wantRequest.headers, gotRequest.headers)
      }
      else -> {
        throw Exception("Unsupported new AuthType!!!")
      }
    }
  }
}
