package org.stellar.anchor.util

import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.Logger
import org.stellar.anchor.Constants.Companion.TEST_HOST_URL
import org.stellar.anchor.Constants.Companion.TEST_JWT_SECRET
import org.stellar.anchor.Constants.Companion.TEST_NETWORK_PASS_PHRASE
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.config.PII
import org.stellar.anchor.util.Log.shorter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LogTest {
  @MockK(relaxed = true) private lateinit var logger: Logger

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxed = true)
    mockkStatic(Log::class)
    every { Log.getLogger() } returns logger
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
  }

  @Suppress("unused")
  class TestBean {
    val field1: String = "1"
    val field2: String = "2"
  }

  @Suppress("unused")
  class TestBeanPII {
    val fieldNoPII: String = "no secret"

    @PII val fieldPII: String = "secret"
  }

  val wantTestPIIJson = """{"fieldNoPII":"no secret"}"""

  @Test
  fun `test log messages`() {
    Log.error("Hello")
    verify { logger.error("Hello") }

    Log.warn("Hello")
    verify { logger.warn("Hello") }

    Log.info("Hello")
    verify { logger.info("Hello") }

    Log.debug("Hello")
    verify { logger.debug("Hello") }

    Log.trace("Hello")
    verify { logger.trace("Hello") }
  }

  @Test
  fun `test log messages with JSON format`() {
    val detail = TestBeanPII()

    Log.error("Hello", detail)
    verify { logger.error("Hello$wantTestPIIJson") }

    Log.warn("Hello", detail)
    verify { logger.warn("Hello$wantTestPIIJson") }

    Log.info("Hello", detail)
    verify { logger.info("Hello$wantTestPIIJson") }

    Log.debug("Hello", detail)
    verify { logger.debug("Hello$wantTestPIIJson") }

    Log.trace("Hello", detail)
    verify { logger.trace("Hello$wantTestPIIJson") }
  }

  @Suppress("unused")
  class TestAppConfig : AppConfig {
    override fun getStellarNetworkPassphrase(): String {
      return TEST_NETWORK_PASS_PHRASE
    }

    override fun getHostUrl(): String {
      return TEST_HOST_URL
    }

    override fun getHorizonUrl(): String {
      return "https://horizon.stellar.org"
    }

    override fun getJwtSecretKey(): String {
      return TEST_JWT_SECRET
    }

    override fun getAssets(): String {
      return "test_assets_file"
    }

    override fun getLanguages(): MutableList<String> {
      return mutableListOf("en")
    }
  }

  @Test
  fun `test errorEx`() {
    Log.errorEx(Exception("mock exception"))
    verify(exactly = 1) { logger.error(any()) }

    val slot = slot<String>()
    every { logger.error(capture(slot)) } answers {}

    Log.errorEx("Hello", Exception("mock exception"))
    assertTrue(slot.captured.contains("Hello"))
    assertTrue(slot.captured.contains("mock exception"))
  }

  @Test
  fun `test shorter string conversion`() {
    assertNull(shorter(null))
    assertEquals(shorter("123"), "123")
    assertEquals(shorter(""), "")
    assertEquals(shorter("12345678"), "12345678")
    assertEquals(shorter("ABCD123ABCD"), "ABCD123ABCD")
    assertEquals(shorter("ABCD123_ABCD"), "ABCD...ABCD")
  }

  @Test
  fun `test getLogger`() {
    unmockkAll()
    val logger = Log.getLogger()
    assertNotNull(logger)
  }
}
