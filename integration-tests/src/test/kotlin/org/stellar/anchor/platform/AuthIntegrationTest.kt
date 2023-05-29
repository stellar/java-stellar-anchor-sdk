package org.stellar.anchor.platform

import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.NotFoundException
import org.stellar.anchor.api.exception.SepNotFoundException
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerRequest
import org.stellar.anchor.api.sep.sep12.Sep12Status
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.auth.AuthType
import org.stellar.anchor.platform.AbstractIntegrationTest.Companion.PLATFORM_TO_ANCHOR_SECRET
import org.stellar.anchor.platform.callback.RestCustomerIntegration
import org.stellar.anchor.util.GsonUtils
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

open class AbstractAuthIntegrationTest {
    companion object {
        internal lateinit var testProfileRunner: TestProfileExecutor
    }

    init {}
}

internal class JwtAuthIntegrationTest : AbstractAuthIntegrationTest() {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            testProfileRunner = TestProfileExecutor(TestConfig(profileName = "default").also {
                it.env["platform_server.auth.type"] = "jwt"
                // todo: start reference server, set auth type to jwt
            })
            testProfileRunner.start()
        }

        @AfterAll
        @JvmStatic
        fun breakdown() {
            testProfileRunner.shutdown()
        }
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .build()

    private val authHelper: AuthHelper = AuthHelper.from(AuthType.JWT, PLATFORM_TO_ANCHOR_SECRET, 30000)
    private val gson = GsonUtils.getInstance()

    // TODO: use the callback clients to call a reference server endpoint with auth enabled
    @Test
    fun `test something happens`() {
        val rci = RestCustomerIntegration("http://localhost:${AbstractIntegrationTest.REFERENCE_SERVER_PORT}", httpClient, authHelper, gson)
        assertThrows<NotFoundException> {
            rci.getCustomer(Sep12GetCustomerRequest.builder().id("1").build())
        }
   }
}