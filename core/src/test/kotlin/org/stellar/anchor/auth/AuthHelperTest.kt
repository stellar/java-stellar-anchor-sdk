package org.stellar.anchor.auth

import io.mockk.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.stellar.anchor.config.IntegrationAuthConfig.AuthType
import org.stellar.anchor.util.AuthHeader

class AuthHelperTest {
  companion object {
    const val JWT_EXPIRATION_MILLISECONDS: Long = 90000
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
  }

  @ParameterizedTest
  @EnumSource(AuthType::class)
  fun `test AuthHeader creation based on the AuthType`(authType: AuthType) {
    when (authType) {
      AuthType.JWT_TOKEN -> {
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
          JwtToken.of(
            "http://localhost:8080",
            currentTimeMilliseconds / 1000L,
            (currentTimeMilliseconds + JWT_EXPIRATION_MILLISECONDS) / 1000L
          )

        val jwtService = JwtService("secret")
        val authHelper =
          AuthHelper.forJwtToken(jwtService, JWT_EXPIRATION_MILLISECONDS, "http://localhost:8080")
        val gotAuthHeader = authHelper.createAuthHeader()
        val wantAuthHeader =
          AuthHeader("Authorization", "Bearer ${jwtService.encode(wantJwtToken)}")
        assertEquals(wantAuthHeader, gotAuthHeader)
      }
      AuthType.API_KEY -> {
        val authHelper = AuthHelper.forApiKey("secret")
        val gotAuthHeader = authHelper.createAuthHeader()
        val wantAuthHeader = AuthHeader("X-Api-Key", "secret")
        assertEquals(wantAuthHeader, gotAuthHeader)
      }
      AuthType.NONE -> {
        val authHelper = AuthHelper.forNone()
        val authHeader = authHelper.createAuthHeader()
        assertNull(authHeader)
      }
      else -> {
        throw Exception("Unsupported new AuthType!!!")
      }
    }
  }
}
