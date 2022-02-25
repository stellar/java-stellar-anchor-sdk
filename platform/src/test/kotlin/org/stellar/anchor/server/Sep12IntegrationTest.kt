package org.stellar.anchor.server

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.reference.AnchorRerferenceServer
import org.stellar.anchor.sep10.JwtService
import org.stellar.anchor.sep10.JwtToken
import org.stellar.anchor.server.configurator.DataAccessConfigurator
import org.stellar.anchor.server.configurator.PlatformAppConfigurator
import org.stellar.anchor.server.configurator.PropertiesReader
import org.stellar.anchor.server.configurator.SpringFrameworkConfigurator

@SpringBootTest(
  classes = [AnchorPlatformServer::class],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(locations = ["classpath:application-integration-test.properties"])
@ContextConfiguration(
  initializers =
    [
      PropertiesReader::class,
      PlatformAppConfigurator::class,
      DataAccessConfigurator::class,
      SpringFrameworkConfigurator::class]
)
class Sep12IntegrationTest {
  @Autowired lateinit var restTemplate: TestRestTemplate
  @Autowired lateinit var jwtService: JwtService
  @Autowired lateinit var appConfig: AppConfig
  @Autowired lateinit var applicationContext: ApplicationContext

  @LocalServerPort var port: Int = 0

  companion object {

    private const val PUBLIC_KEY = "GBJDSMTMG4YBP27ZILV665XBISBBNRP62YB7WZA2IQX2HIPK7ABLF4C2"
    private val client =
      OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .callTimeout(10, TimeUnit.MINUTES)
        .build()

    @BeforeAll
    @JvmStatic
    fun setup() {
      val ars =
        SpringApplicationBuilder(AnchorRerferenceServer::class.java)
          .properties("server.port=8081", "server.contextPath=/")
      ars.run()
    }
  }

  @Test
  fun runTest() {
    //    val port = applicationContext.environment.getProperty("server.port")
    val jwtToken = createJwtToken()
    val request =
      Request.Builder()
        .url("http://localhost:$port/sep12/customer")
        .header("Authorization", "Bearer $jwtToken")
        .get()
        .build()

    client.newCall(request).execute().use { response -> println(response.body!!.string()) }
  }

  private fun createJwtToken(): String {
    val issuedAt: Long = System.currentTimeMillis() / 1000L
    val jwtToken =
      JwtToken.of(appConfig.hostUrl + "/auth", PUBLIC_KEY, issuedAt, issuedAt + 60, "", null)
    return jwtService.encode(jwtToken)
  }
}
