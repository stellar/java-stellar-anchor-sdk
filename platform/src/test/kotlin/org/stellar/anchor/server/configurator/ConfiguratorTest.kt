package org.stellar.anchor.server.configurator

import io.mockk.every
import io.mockk.spyk
import io.mockk.unmockkAll
import java.io.File
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.get
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
    every { propertiesReader[method]() } returns "classpath:test-anchor-config.yaml"

    propertiesReader.initialize(applicationContext)
    loadConfigurations(applicationContext)
    testYamlProperties(applicationContext)
  }

  @Test
  fun testFromUserFolder() {
    val applicationContext = AnnotationConfigApplicationContext()
    val propertiesReader = spyk<PropertiesReader>(recordPrivateCalls = true)
    every { propertiesReader.getFromUserFolder() } returns
      ResourceUtils.getFile("classpath:test-anchor-config.yaml")

    propertiesReader.initialize(applicationContext)
    loadConfigurations(applicationContext)
    testYamlProperties(applicationContext)

    every { propertiesReader.getFromUserFolder() } returns File("bad file")
    assertThrows<java.lang.IllegalArgumentException> {
      propertiesReader.initialize(applicationContext)
    }
  }

  @Test
  fun testGetFromUserFolder() {
    val file = PropertiesReader().fromUserFolder
    assertTrue(file.absolutePath.endsWith(".anchor/anchor-config.yaml"))
  }

  fun loadConfigurations(context: ConfigurableApplicationContext) {
    PlatformAppConfigurator().initialize(context)
    DataAccessConfigurator().initialize(context)
    SpringFrameworkConfigurator().initialize(context)
  }

  fun testYamlProperties(context: ConfigurableApplicationContext) {
    val tests =
      mapOf(
        "sep1.enabled" to "true",
        "sep10.enabled" to "true",
        "sep10.homeDomain" to "localhost:8080",
        "sep10.signingSeed" to "SAX3AH622R2XT6DXWWSRIDCMMUCCMATBZ5U6XKJWDO7M2EJUBFC3AW5X",
        "sep24.interactiveJwtExpiration" to "300",
        "sep24.interactiveUrl" to "http://localhost:8080/ref/sep24/interactive",
        "payment-gateway.circle.name" to "circle",
        "payment-gateway.circle.stellarNetwork" to "TESTNET",
        "spring.jpa.database-platform" to "org.stellar.anchor.server.sqlite.SQLiteDialect",
        "logging.level.root" to "INFO",
        "server.servlet.context-path" to "/"
      )

    tests.forEach { Assertions.assertEquals(it.value, context.environment[it.key]) }
  }
}
