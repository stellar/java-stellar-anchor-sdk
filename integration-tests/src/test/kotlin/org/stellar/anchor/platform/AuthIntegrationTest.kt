package org.stellar.anchor.platform

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.callback.GetFeeRequest
import org.stellar.anchor.api.callback.GetRateRequest
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.NotFoundException
import org.stellar.anchor.api.exception.ServerErrorException
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerRequest
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.platform.AbstractIntegrationTest.Companion.ANCHOR_TO_PLATFORM_SECRET
import org.stellar.anchor.platform.AbstractIntegrationTest.Companion.JWT_EXPIRATION_MILLISECONDS
import org.stellar.anchor.platform.AbstractIntegrationTest.Companion.PLATFORM_TO_ANCHOR_SECRET
import org.stellar.anchor.platform.callback.RestCustomerIntegration
import org.stellar.anchor.platform.callback.RestFeeIntegration
import org.stellar.anchor.platform.callback.RestRateIntegration
import org.stellar.anchor.util.GsonUtils

open class AbstractAuthIntegrationTest {
  companion object {
    val jwtService =
      JwtService(null, null, null, PLATFORM_TO_ANCHOR_SECRET, ANCHOR_TO_PLATFORM_SECRET)
    val jwtAuthHelper: AuthHelper = AuthHelper.forJwtToken(jwtService, JWT_EXPIRATION_MILLISECONDS)
    internal val nonAuthHelper = AuthHelper.forNone()
    internal lateinit var testProfileRunner: TestProfileExecutor
  }

  init {}
}

internal class JwtAuthIntegrationTest : AbstractAuthIntegrationTest() {
  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      testProfileRunner =
        TestProfileExecutor(
          TestConfig(profileName = "default").also {
            it.env["platform_server.auth.type"] = "jwt"
            it.env["integration-auth.authType"] = "jwt"
            it.env["integration-auth.platformToAnchorSecret"] = PLATFORM_TO_ANCHOR_SECRET
            it.env["integration-auth.anchorToPlatformSecret"] = ANCHOR_TO_PLATFORM_SECRET
            it.env["integration-auth.expirationMilliseconds"] =
              JWT_EXPIRATION_MILLISECONDS.toString()
          }
        )
      testProfileRunner.start()
    }

    @AfterAll
    @JvmStatic
    fun breakdown() {
      testProfileRunner.shutdown()
    }
  }

  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.MINUTES)
      .readTimeout(10, TimeUnit.MINUTES)
      .writeTimeout(10, TimeUnit.MINUTES)
      .build()

  private val gson = GsonUtils.getInstance()

  @Test
  fun `test the callback customer endpoint with JWT auth`() {
    val rci =
      RestCustomerIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        jwtAuthHelper,
        gson
      )
    assertThrows<NotFoundException> {
      rci.getCustomer(Sep12GetCustomerRequest.builder().id("1").build())
    }
  }

  @Test
  fun `test the callback rate endpoint with JWT auth`() {
    val rri =
      RestRateIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        jwtAuthHelper,
        gson
      )
    assertThrows<BadRequestException> { rri.getRate(GetRateRequest.builder().build()) }
  }

  @Test
  fun `test the callback fee endpoint with JWT auth`() {
    val rfi =
      RestFeeIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        jwtAuthHelper,
        gson
      )
    assertThrows<BadRequestException> { rfi.getFee(GetFeeRequest.builder().build()) }
  }

  @Test
  fun `test JWT protection of callback customer endpoint`() {
    val rci =
      RestCustomerIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        nonAuthHelper,
        gson
      )
    assertThrows<ServerErrorException> {
      rci.getCustomer(Sep12GetCustomerRequest.builder().id("1").build())
    }
  }

  @Test
  fun `test JWT protection of callback rate endpoint`() {
    val rri =
      RestRateIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        nonAuthHelper,
        gson
      )
    assertThrows<ServerErrorException> { rri.getRate(GetRateRequest.builder().build()) }
  }

  @Test
  fun `test JWT protection of callback fee endpoint`() {
    val rfi =
      RestFeeIntegration(
        "http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}",
        httpClient,
        nonAuthHelper,
        gson
      )
    assertThrows<ServerErrorException> { rfi.getFee(GetFeeRequest.builder().build()) }
  }
}
