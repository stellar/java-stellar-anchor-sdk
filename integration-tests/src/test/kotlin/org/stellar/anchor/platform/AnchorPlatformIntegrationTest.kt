package org.stellar.anchor.platform

import com.palantir.docker.compose.DockerComposeExtension
import com.palantir.docker.compose.connection.waiting.HealthChecks
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.stellar.anchor.util.Sep1Helper
import org.stellar.anchor.util.Sep1Helper.TomlContent

data class TestConfig(
  val referenceServerPort: Int,
  val sepServerPort: Int,
  val observerHealthServerPort: Int,
  val sep10JwtSecret: String,
  val sep24InteractiveUrlJwtSecret: String,
  val sep24MoreInfoUrlJwtSecret: String
) {}

open class DefaultIntegrationTest(val config: TestConfig) {
  init {
    System.getProperties()
      .setProperty("REFERENCE_SERVER_CONFIG", "classpath:/anchor-reference-server.yaml")
  }

  private val shouldStartDockerCompose =
    System.getenv().getOrDefault("START_DOCKER_COMPOSE", "false").toBoolean()
  private val shouldStartServers =
    System.getenv().getOrDefault("START_ALL_SERVERS", "true").toBoolean()
  private val runningServers = mutableListOf<ConfigurableApplicationContext>()
  lateinit var toml: TomlContent
  lateinit var jwt: String
  lateinit var sep10Tests: Sep10Tests
  lateinit var sep12Tests: Sep12Tests
  lateinit var sep24Tests: Sep24Tests
  lateinit var sep31Tests: Sep31Tests
  lateinit var sep38Tests: Sep38Tests

  val docker: DockerComposeExtension =
    DockerComposeExtension.builder()
      .saveLogsTo("build/docker-logs/anchor-platform-integration-test")
      .file("src/test/resources/test-default//docker-compose.yaml")
      .waitingForService("kafka", HealthChecks.toHaveAllPortsOpen())
      .waitingForService("db", HealthChecks.toHaveAllPortsOpen())
      .pullOnStartup(true)
      .build()

  fun _setUp() {
    if (shouldStartDockerCompose) startDocker()
    if (shouldStartServers) startServers()
  }

  fun _tearDown() {
    if (shouldStartServers) stopServers()
    if (shouldStartDockerCompose) stopDocker()
  }

  fun startDocker() {
    docker.beforeAll(null)
  }

  private fun startServers() = runBlocking {
    val envMap = readResourceAsMap("test-default/env")

    envMap["data.type"] = "h2"
    envMap["events.enabled"] = "false"
    envMap["assets.value"] = getResourceFilePath("test-default/assets.yaml")
    envMap["sep1.toml.value"] = getResourceFilePath("test-default/stellar.toml")

    // Start servers
    val jobs = mutableListOf<Job>()
    val scope = CoroutineScope(Dispatchers.Default)
    jobs += scope.launch { ServiceRunner.startKotlinReferenceServer(false) }
    jobs += scope.launch { runningServers.add(ServiceRunner.startAnchorReferenceServer()) }
    jobs += scope.launch { runningServers.add(ServiceRunner.startStellarObserver(envMap)) }
    jobs += scope.launch { runningServers.add(ServiceRunner.startSepServer(envMap)) }
    jobs.forEach { it.join() }

    // Query SEP-1
    toml =
      Sep1Helper.parse(
        resourceAsString("http://localhost:${config.sepServerPort}/.well-known/stellar.toml")
      )

    // Create Sep10Tests
    sep10Tests = Sep10Tests(toml)

    // Get JWT
    if (!::jwt.isInitialized) {
      jwt = sep10Tests.sep10Client.auth()
    }

    sep12Tests = Sep12Tests(config, toml, jwt)
    sep24Tests = Sep24Tests(config, toml, jwt)
    sep31Tests = Sep31Tests(config, toml, jwt)
    sep38Tests = Sep38Tests(config, toml, jwt)
    //    PlatformApiTests.setup()
  }

  fun stopServers() {
    runningServers.forEach { SpringApplication.exit(it) }
    org.stellar.reference.stop()
  }

  fun stopDocker() {
    docker.afterAll(null)
  }
}

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AnchorPlatformIntegrationTest :
  DefaultIntegrationTest(
    TestConfig(
      referenceServerPort = 8081,
      sepServerPort = 8080,
      observerHealthServerPort = 8083,
      sep10JwtSecret = "secret",
      sep24InteractiveUrlJwtSecret = "secret_sep24_interactive_url_jwt_secret",
      sep24MoreInfoUrlJwtSecret = "secret_sep24_more_info_url_jwt_secret"
    )
  ) {
  companion object {
    private val instance = AnchorPlatformIntegrationTest()

    @BeforeAll
    @JvmStatic
    fun setup() {
      instance._setUp()
    }

    @AfterAll
    @JvmStatic
    fun tearDown() {
      instance._tearDown()
    }
  }

  @Test
  @Order(1)
  fun runSep10Test() {
    instance.sep10Tests.testAll()
  }

  @Test
  @Order(2)
  fun runSep12Test() {
    instance.sep12Tests.testAll()
  }

  @Test
  @Order(3)
  fun runSep24Test() {
    instance.sep24Tests.testAll()
  }

  @Test
  @Order(4)
  fun runSep31Test() {
    instance.sep31Tests.testAll()
  }

  @Test
  @Order(5)
  fun runSep38Test() {
    //    sep38TestAll()
  }

  @Test
  @Order(6)
  fun runPlatformApiTest() {
    //    platformTestAll()
  }

  @Test
  @Order(7)
  fun runCallbackApiTest() {
    //    callbackApiTestAll()
  }

  @Test
  @Order(8)
  fun runStellarObserverTest() {
    //    stellarObserverTestAll()
  }
}
