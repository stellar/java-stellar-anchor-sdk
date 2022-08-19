package org.stellar.anchor.platform.configurator

import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.core.io.ClassPathResource
import org.stellar.anchor.api.exception.InvalidConfigException
import org.stellar.anchor.platform.configurator.ConfigMap.ConfigSource.DEFAULT
import org.stellar.anchor.platform.configurator.ConfigMap.ConfigSource.FILE

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ConfigManagerTest {
  lateinit var configManager: ConfigManager

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    mockkStatic(ConfigReader::class)
    mockkStatic(ConfigHelper::class)

    configManager = spyk(ConfigManager.getInstance())

    every { ConfigHelper.loadDefaultConfig() } returns
      ConfigHelper.loadConfig(
        ClassPathResource("org/stellar/anchor/platform/configurator/def/config-defaults-v3.yaml"),
        DEFAULT
      )

    every { ConfigReader.getVersionDefFile(any()) } answers
      {
        String.format(
          "org/stellar/anchor/platform/configurator/def/config-def-v%d.yaml",
          firstArg<Int>()
        )
      }
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  @Order(1)
  fun `(scene-1) configuration with version upgrades`() {
    every { configManager.getConfigFileAsResource(any()) } answers
      {
        ClassPathResource("org/stellar/anchor/platform/configurator/scene-1/test.yaml")
      }

    val wantedConfig =
      ConfigHelper.loadConfig(
        ClassPathResource("org/stellar/anchor/platform/configurator/scene-1/wanted.yaml"),
        FILE
      )
    val gotConfig = configManager.processConfigurations(null)

    assertTrue(gotConfig.sameAs(wantedConfig))
  }

  @Test
  @Order(2)
  fun `(scene-2) bad configuration file`() {
    every { configManager.getConfigFileAsResource(any()) } answers
      {
        ClassPathResource("org/stellar/anchor/platform/configurator/scene-2/test.bad.yaml")
      }
    val ex = assertThrows<InvalidConfigException> { configManager.processConfigurations(null) }
    assertEquals(2, ex.messages.size)
    assertEquals("Invalid configuration: stellar.apollo=star. (version=1)", ex.messages[0])
    assertEquals("Invalid configuration: horizon.aster=star. (version=1)", ex.messages[1])
  }

  @Test
  @Order(3)
  fun `(scene-3) configuration from file and system environment variables with upgrades`() {
    every { configManager.getConfigFileAsResource(any()) } answers
      {
        ClassPathResource("org/stellar/anchor/platform/configurator/scene-3/test.yaml")
      }

    System.setProperty("stellar.bianca", "white")
    System.setProperty("stellar.deimos", "satellite")

    ConfigEnvironment.reset()

    val wantedConfig =
      ConfigHelper.loadConfig(
        ClassPathResource("org/stellar/anchor/platform/configurator/scene-3/wanted.yaml"),
        FILE
      )
    val gotConfig = configManager.processConfigurations(null)

    assertTrue(gotConfig.sameAs(wantedConfig))
  }
}
