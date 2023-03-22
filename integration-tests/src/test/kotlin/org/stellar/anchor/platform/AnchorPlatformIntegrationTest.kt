package org.stellar.anchor.platform

import com.palantir.docker.compose.DockerComposeExtension
import com.palantir.docker.compose.connection.waiting.HealthChecks
import org.junit.jupiter.api.*
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.stellar.anchor.util.Sep1Helper
import org.stellar.anchor.util.Sep1Helper.TomlContent

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformIntegrationTest {
  companion object {
    lateinit var toml: TomlContent
    lateinit var jwt: String

    const val REFERENCE_SERVER_PORT = 8081
    const val SEP_SERVER_PORT = 8080
    const val OBSERVER_HEALTH_SERVER_PORT = 8083
    const val SEP10_JWT_SECRET = "secret"
    const val SEP24_INTERACTIVE_URL_JWT_SECRET = "secret_sep24_interactive_url_jwt_secret"
    const val SEP24_MORE_INFO_URL_JWT_SECRET = "secret_sep24_more_info_url_jwt_secret"

    private val shouldStartDockerCompose =
        System.getenv().getOrDefault("START_DOCKER_COMPOSE", "false").toBoolean()
    private val shouldStartServers =
        System.getenv().getOrDefault("START_ALL_SERVERS", "true").toBoolean()

    init {
      val props = System.getProperties()
      props.setProperty("REFERENCE_SERVER_CONFIG", "classpath:/anchor-reference-server.yaml")
    }

    val docker: DockerComposeExtension =
        DockerComposeExtension.builder()
            .saveLogsTo("build/docker-logs/anchor-platform-integration-test")
            .file("src/test/resources/test-default//docker-compose.yaml")
            .waitingForService("kafka", HealthChecks.toHaveAllPortsOpen())
            .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
            .pullOnStartup(true)
            .build()

    val runningServers = mutableListOf<ConfigurableApplicationContext>()

    @BeforeAll
    @JvmStatic
    fun beforeAllTests() {
      if (shouldStartDockerCompose) startDocker()
      if (shouldStartServers) startServers()
    }

    private fun startDocker() {
      docker.beforeAll(null)
    }

    private fun startServers() {
      val envMap = readResourceAsMap("test-default/env")

      envMap["data.type"] = "h2"
      envMap["events.enabled"] = "false"
      envMap["assets.value"] = getResourceFilePath("test-default/assets.yaml")
      envMap["sep1.toml.value"] = getResourceFilePath("test-default/stellar.toml")

      ServiceRunner.startKotlinReferenceServer(false)
      runningServers.add(ServiceRunner.startAnchorReferenceServer())
      runningServers.add(ServiceRunner.startStellarObserver(envMap))
      runningServers.add(ServiceRunner.startSepServer(envMap))

      toml =
          Sep1Helper.parse(
              resourceAsString("http://localhost:$SEP_SERVER_PORT/.well-known/stellar.toml"))

      Sep10Tests.setup()

      if (!::jwt.isInitialized) {
        jwt = sep10Client.auth()
      }

      Sep12Tests.setup()
      Sep24Tests.setup()
      Sep31Tests.setup()
      Sep38Tests.setup()
      PlatformApiTests.setup()
    }

    @AfterAll
    @JvmStatic
    fun afterAllTests() {
      if (shouldStartServers) stopServers()
      if (shouldStartDockerCompose) stopDocker()
    }

    fun stopServers() {
      runningServers.forEach { SpringApplication.exit(it) }
      org.stellar.reference.stop()
    }

    fun stopDocker() {
      docker.afterAll(null)
    }
  }

  @Test
  @Order(1)
  fun runSep10Test() {
    sep10TestAll()
  }

  @Test
  @Order(2)
  fun runSep12Test() {
    sep12TestAll()
  }

  @Test
  @Order(3)
  fun runSep24Test() {
    sep24TestAll()
  }

  @Test
  @Order(4)
  fun runSep31Test() {
    sep31TestAll()
  }

  @Test
  @Order(5)
  fun runSep38Test() {
    sep38TestAll()
  }

  @Test
  @Order(6)
  fun runPlatformApiTest() {
    platformTestAll()
  }

  @Test
  @Order(7)
  fun runCallbackApiTest() {
    callbackApiTestAll()
  }

  @Test
  @Order(8)
  fun runStellarObserverTest() {
    stellarObserverTestAll()
  }
}
