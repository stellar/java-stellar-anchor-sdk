package org.stellar.anchor.platform.configurator

import io.mockk.every
import io.mockk.spyk
import io.mockk.unmockkAll
import java.io.File
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
  fun testReadFromUserFolder() {
    val applicationContext = AnnotationConfigApplicationContext()
    val propertiesReader = spyk<PropertiesReader>(recordPrivateCalls = true)
    every { propertiesReader.getFromUserFolder() } returns
      ResourceUtils.getFile("classpath:test-read.yaml")

    propertiesReader.initialize(applicationContext)
    loadConfigurations(applicationContext)

    every { propertiesReader.getFromUserFolder() } returns File("bad file")
    assertThrows<java.lang.IllegalArgumentException> {
      propertiesReader.initialize(applicationContext)
    }
  }

  @Test
  fun testFromUserFolder() {
    val file = PropertiesReader().fromUserFolder
    assertTrue(
      file.absolutePath.endsWith(String.format(".anchor%sanchor-config.yaml", File.separator))
    )
  }

  fun loadConfigurations(context: ConfigurableApplicationContext) {
    PlatformAppConfigurator().initialize(context)
    DataAccessConfigurator().initialize(context)
    SpringFrameworkConfigurator().initialize(context)
  }
}
