package org.stellar.anchor.util

import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.slf4j.Logger
import org.stellar.anchor.util.Log.gson
import org.stellar.anchor.util.Log.shorter
import java.beans.IntrospectionException
import java.beans.Introspector

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LogTest {
    @MockK(relaxed = true)
    private lateinit var logger: Logger

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

    @Test
    fun testInfoDebug() {
        Log.info("Hello")
        verify {
            logger.info("Hello")
        }

        Log.debug("Hello")
        verify {
            logger.debug("Hello")
        }
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
        verify(exactly = 2) {
            logger.info(ofType(String::class))
        }
    }

    @Test
    fun testInfoDebugF() {
        Log.infoF("Hello {}", "world")
        verify(exactly = 1) {
            logger.info(ofType(String::class), *anyVararg())
        }

        Log.debugF("Hello {}", "world")
        verify(exactly = 1) {
            logger.debug(ofType(String::class), *anyVararg())
        }
    }

    @Test
    fun testInfoBException() {
        mockkStatic(Introspector::class)
        every { Introspector.getBeanInfo(any()) } answers { throw IntrospectionException("mocked exception") }

        val testBean = TestBean()
        Log.infoB("Hello", testBean)

        verify(exactly = 2) {
            logger.info(ofType(String::class))
        }
    }

    @Test
    fun testErrorEx() {
        Log.errorEx(Exception("mock exception"))

        verify(exactly = 1) {
            logger.error(any())
        }
    }

    @Test
    fun testShorter() {
        assertThrows<NullPointerException> {
            shorter(null)
        }
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