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
    const val SEP10_JWT_SECRET = "secret"
    const val SEP24_INTERACTIVE_URL_JWT_SECRET = "sep24 interactive url secret"
    const val SEP24_MORE_INFO_URL_JWT_SECRET = "sep24 more_info url secret"

    init {
      val props = System.getProperties()
      props.setProperty("REFERENCE_SERVER_CONFIG", "classpath:/anchor-reference-server.yaml")
    }

    @BeforeAll
    @JvmStatic
    fun startServers() {
      val envMap =
        mapOf(
          "stellar_anchor_config" to "classpath:integration-test.anchor-config.yaml",
          "sep_server.port" to SEP_SERVER_PORT,
          "sep_server.context_path" to "/",
          "payment_observer.port" to OBSERVER_HEALTH_SERVER_PORT,
          "payment_observer.context_path" to "/",
          "secret.sep10.signing_seed" to "SAKXNWVTRVR4SJSHZUDB2CLJXEQHRT62MYQWA2HBB7YBOTCFJJJ55BZF",
          "secret.sep10.jwt_secret" to SEP10_JWT_SECRET,
          "secret.sep24.interactive_url.jwt_secret" to SEP24_INTERACTIVE_URL_JWT_SECRET,
          "secret.sep24.more_info_url.jwt_secret" to SEP24_MORE_INFO_URL_JWT_SECRET,
          "secret.data.username" to "user1",
          "secret.data.password" to "password",
          "secret.callback_api.auth_secret" to "callback_jwt_secret",
          "secret.platform_api.auth_secret" to "platform_jwt_secret",
          // The events and kafka should be tested in e2e tests.
          "events.enabled" to false,
          "sep24.port" to "8091",
          "sep24.anchorPlatformUrl" to "http://localhost:8080",
          "sep24.horizonUrl" to "https://horizon-testnet.stellar.org",
          "sep24.secret" to "SDYGC4TW5HHR5JA6CB2XLTTBF2DZRH2KDPBDPV3D5TXM6GF7FBPRZF3I",
          "sep24.mode" to "test"
        )

      ServiceRunner.startAnchorReferenceServer(false)
      ServiceRunner.startStellarObserver(envMap)
      ServiceRunner.startSepServer(envMap)

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
