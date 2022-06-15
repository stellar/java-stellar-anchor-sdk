package org.stellar.anchor.platform.configurator

import io.mockk.every
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.File
import java.io.IOException
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.util.ResourceUtils

open class ConfiguratorTest {
  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @ParameterizedTest
  @ValueSource(strings = ["getFromSystemEnv", "getFromSystemProperty"])
  fun testSystemEnv(method: String) {
    val applicationContext = AnnotationConfigApplicationContext()
    val propertiesReader = spyk<PropertiesReader>(recordPrivateCalls = true)
    every { propertiesReader[method]() } returns "classpath:test-read.yaml"

    propertiesReader.initialize(applicationContext)
    loadConfigurations(applicationContext)
  }

  @Test
  fun testReadFromUserFolder_fileExists() {
    val applicationContext = AnnotationConfigApplicationContext()
    val propertiesReader = spyk<PropertiesReader>(recordPrivateCalls = true)

    every { propertiesReader.getFromUserFolder() } returns
      ResourceUtils.getFile("classpath:test-read.yaml")
    assertDoesNotThrow { propertiesReader.initialize(applicationContext) }
    verify(exactly = 1) { propertiesReader.getFromUserFolder() }
    loadConfigurations(applicationContext)
  }

  @Test
  fun testReadFromUserFolder_nonExistentFileDefaultsToEnv() {
    val applicationContext = AnnotationConfigApplicationContext()
    val propertiesReader = spyk<PropertiesReader>(recordPrivateCalls = true)

    every { propertiesReader.getFromUserFolder() } returns File("bad file")
    assertDoesNotThrow { propertiesReader.initialize(applicationContext) }
    verify(exactly = 1) { propertiesReader.getFromUserFolder() }
    loadConfigurations(applicationContext)
  }

  @Test
  fun testGetFromSystemEnv_fileExists() {
    val applicationContext = AnnotationConfigApplicationContext()
    val propertiesReader = spyk<PropertiesReader>(recordPrivateCalls = true)

    every { propertiesReader.fromUserFolder } returns File("not exist")
    every { propertiesReader.getFromSystemEnv() } returns "classpath:test-read.yaml"
    assertDoesNotThrow { propertiesReader.initialize(applicationContext) }
    verify(exactly = 1) { propertiesReader.getFromSystemEnv() }
    loadConfigurations(applicationContext)
  }

  @Test
  fun testGetFromSystemEnv_nonExistentFileDefaultsToEnv() {
    val applicationContext = AnnotationConfigApplicationContext()
    val propertiesReader = spyk<PropertiesReader>(recordPrivateCalls = true)

    every { propertiesReader.getFromSystemEnv() } returns "bad file path"
    val ex = assertThrows<IOException> { propertiesReader.initialize(applicationContext) }
    assertInstanceOf(IOException::class.java, ex)
    assertEquals("Resource not found", ex.message)
    verify(exactly = 1) { propertiesReader.getFromSystemEnv() }
  }

  @Test
  fun testFromUserFolder() {
    val file = PropertiesReader().fromUserFolder
    assertTrue(
      file.absolutePath.endsWith(String.format(".anchor%sanchor-config.yaml", File.separator))
    )
  }

  private fun loadConfigurations(context: ConfigurableApplicationContext) {
    PlatformAppConfigurator().initialize(context)
    DataAccessConfigurator().initialize(context)
    SpringFrameworkConfigurator().initialize(context)
  }
}
