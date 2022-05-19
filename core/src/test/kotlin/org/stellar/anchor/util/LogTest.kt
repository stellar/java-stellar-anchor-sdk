package org.stellar.anchor.util

import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.beans.IntrospectionException
import java.beans.Introspector
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.slf4j.Logger
import org.stellar.anchor.Constants.Companion.TEST_HOST_URL
import org.stellar.anchor.Constants.Companion.TEST_JWT_SECRET
import org.stellar.anchor.Constants.Companion.TEST_NETWORK_PASS_PHRASE
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.config.PII
import org.stellar.anchor.util.Log.gson
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

  @Test
  fun testInfoDebug() {
    Log.info("Hello")
    verify { logger.info("Hello") }

    Log.debug("Hello")
    verify { logger.debug("Hello") }
  }

  @Test
  fun testInfoDebug2() {
    val detail = Object()
    Log.info("Hello", detail)
    verify {
      logger.info("Hello")
      logger.info(gson.toJson(detail))
    }

    Log.debug("Hello", detail)
    verify {
      logger.debug("Hello")
      logger.debug(gson.toJson(detail))
    }
  }

  @Test
  fun testInfoB() {
    val testBean = TestBean()
    Log.infoB("Hello", testBean)
    verify(exactly = 2) { logger.info(ofType(String::class)) }
  }

  @Test
  fun testInfoBPII() {
    val slotMessages = mutableListOf<String>()
    every { logger.info(capture(slotMessages)) } answers {}

    val testBeanPII = TestBeanPII()
    Log.infoB("Hello", testBeanPII)

    verify(exactly = 2) { logger.info(ofType(String::class)) }
    assertEquals("Hello", slotMessages[0])
    val wantBean = """{
'fieldNoPII': 'no secret'
}""".trimMargin()
    assertEquals(wantBean, slotMessages[1])
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
  fun testInfoConfig() {
    val slot = slot<String>()
    every { logger.info(capture(slot)) } answers {}

    val testAppConfig = TestAppConfig()
    Log.infoConfig("Hello", testAppConfig, AppConfig::class.java)
    verify(exactly = 2) { logger.info(ofType(String::class)) }
    assertFalse(slot.captured.contains(TEST_JWT_SECRET))
  }

  @Test
  fun testInfoDebugF() {
    Log.infoF("Hello {}", "world")
    verify(exactly = 1) { logger.info(ofType(String::class), *anyVararg()) }

    Log.debugF("Hello {}", "world")
    verify(exactly = 1) { logger.debug(ofType(String::class), *anyVararg()) }
  }

  @Test
  fun testInfoBException() {
    mockkStatic(Introspector::class)
    every { Introspector.getBeanInfo(any()) } answers
      {
        throw IntrospectionException("mocked exception")
      }

    val testBean = TestBean()
    Log.infoB("Hello", testBean)

    verify(exactly = 2) { logger.info(ofType(String::class)) }
  }

  @Test
  fun testErrorEx() {
    Log.errorEx(Exception("mock exception"))
    verify(exactly = 1) { logger.error(any()) }

    val slot = slot<String>()
    every { logger.error(capture(slot)) } answers {}

    Log.errorEx("Hello", Exception("mock exception"))
    assertTrue(slot.captured.contains("Hello"))
    assertTrue(slot.captured.contains("mock exception"))
  }

  @Test
  fun testShorter() {
    assertThrows<NullPointerException> { shorter(null) }
    assertEquals(shorter("123"), "123")
    assertEquals(shorter(""), "")
    assertEquals(shorter("12345678"), "12345678")
    assertEquals(shorter("ABCD123ABCD"), "ABCD123ABCD")
    assertEquals(shorter("ABCD123_ABCD"), "ABCD...ABCD")
  }

  @Test
  fun testGetLogger() {
    unmockkAll()
    val logger = Log.getLogger()
    assertNotNull(logger)
  }
}
