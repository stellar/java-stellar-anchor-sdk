package org.stellar.anchor.auth

import io.mockk.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.stellar.anchor.auth.ApiAuthJwt.*
import org.stellar.anchor.auth.AuthType.*
import org.stellar.anchor.util.AuthHeader

@Execution(SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
  @Execution(SAME_THREAD)
  fun `test AuthHeader creation based on the AuthType`(authType: AuthType) {
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
        val wantPlatformJwt =
          PlatformAuthJwt(
            currentTimeMilliseconds / 1000L,
            (currentTimeMilliseconds + JWT_EXPIRATION_MILLISECONDS) / 1000L
          )
        val wantCallbackJwt =
          CallbackAuthJwt(
            currentTimeMilliseconds / 1000L,
            (currentTimeMilliseconds + JWT_EXPIRATION_MILLISECONDS) / 1000L
          )
        val wantCustodyJwt =
          CustodyAuthJwt(
            currentTimeMilliseconds / 1000L,
            (currentTimeMilliseconds + JWT_EXPIRATION_MILLISECONDS) / 1000L
          )

        val jwtService = JwtService(null, null, null, "secret", "secret", "secret")
        val authHelper = AuthHelper.forJwtToken(jwtService, JWT_EXPIRATION_MILLISECONDS)
        val gotPlatformAuthHeader = authHelper.createPlatformServerAuthHeader()
        val wantPlatformAuthHeader =
          AuthHeader("Authorization", "Bearer ${jwtService.encode(wantPlatformJwt)}")
        assertEquals(wantPlatformAuthHeader, gotPlatformAuthHeader)
        val gotCallbackAuthHeader = authHelper.createCallbackAuthHeader()
        val wantCallbackAuthHeader =
          AuthHeader("Authorization", "Bearer ${jwtService.encode(wantCallbackJwt)}")
        assertEquals(wantCallbackAuthHeader, gotCallbackAuthHeader)
        val gotCustodyAuthHeader = authHelper.createCustodyAuthHeader()
        val wantCustodyAuthHeader =
          AuthHeader("Authorization", "Bearer ${jwtService.encode(wantCustodyJwt)}")
        assertEquals(wantCustodyAuthHeader, gotCustodyAuthHeader)
      }
      API_KEY -> {
        val authHelper = AuthHelper.forApiKey("secret")
        val gotPlatformAuthHeader = authHelper.createPlatformServerAuthHeader()
        val wantPlatformAuthHeader = AuthHeader("X-Api-Key", "secret")
        assertEquals(wantPlatformAuthHeader, gotPlatformAuthHeader)
        val gotCallbackAuthHeader = authHelper.createCallbackAuthHeader()
        val wantCallbackAuthHeader = AuthHeader("X-Api-Key", "secret")
        assertEquals(wantCallbackAuthHeader, gotCallbackAuthHeader)
        val gotCustodyAuthHeader = authHelper.createCustodyAuthHeader()
        val wantCustodyAuthHeader = AuthHeader("X-Api-Key", "secret")
        assertEquals(wantCustodyAuthHeader, gotCustodyAuthHeader)
      }
      NONE -> {
        val authHelper = AuthHelper.forNone()
        val platformAuthHeader = authHelper.createPlatformServerAuthHeader()
        assertNull(platformAuthHeader)
        val callbackAuthHeader = authHelper.createCallbackAuthHeader()
        assertNull(callbackAuthHeader)
        val custodyAuthHeader = authHelper.createCustodyAuthHeader()
        assertNull(custodyAuthHeader)
      }
      else -> {
        throw Exception("Unsupported new AuthType!!!")
      }
    }
  }
}
