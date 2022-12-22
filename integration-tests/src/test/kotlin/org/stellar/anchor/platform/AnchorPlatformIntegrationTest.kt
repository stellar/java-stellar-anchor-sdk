package org.stellar.anchor.platform

import org.junit.jupiter.api.*
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

    init {
      val props = System.getProperties()
      props.setProperty("REFERENCE_SERVER_CONFIG", "classpath:/anchor-reference-server.yaml")
    }

    @BeforeAll
    @JvmStatic
    fun startServers() {
      val envMap =
        mapOf(
          "sep_server.port" to SEP_SERVER_PORT,
          "sep_server.context_path" to "/",
          "payment_observer.port" to OBSERVER_HEALTH_SERVER_PORT,
          "payment_observer.context_path" to "/",
          "stellar_anchor_config" to "classpath:integration-test.anchor-config.yaml",
          "secret.sep10.jwt_secret" to "secret",
          "secret.sep10.signing_seed" to "SAKXNWVTRVR4SJSHZUDB2CLJXEQHRT62MYQWA2HBB7YBOTCFJJJ55BZF",
          "secret.data.username" to "user1",
          "secret.data.password" to "password"
        )

      //      ServiceRunner.startSepServer(envMap)
      //      ServiceRunner.startStellarObserver(envMap)
      //      ServiceRunner.startAnchorReferenceServer()

      toml =
        Sep1Helper.parse(
          resourceAsString("http://localhost:$SEP_SERVER_PORT/.well-known/stellar.toml")
        )

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
