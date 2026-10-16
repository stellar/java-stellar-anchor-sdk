package org.stellar.anchor.platform.integrationtest

import com.moandjiezana.toml.Toml
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.platform.TestConfig

/**
 * `/.well-known/stellar.toml` over HTTP: media types, content type, the served bytes, size, CORS,
 * HEAD and the landing page. Nothing here needs a token, so the class does not extend
 * `IntegrationTestBase` (which authenticates).
 */
class Sep1Tests {
  private val config = TestConfig()
  private val domain = config.env["anchor.domain"]!!
  private val tomlUrl = "$domain/.well-known/stellar.toml"

  private fun <T> withClient(followRedirects: Boolean = true, block: suspend (HttpClient) -> T): T =
    runBlocking {
      HttpClient { this.followRedirects = followRedirects }.use { block(it) }
    }

  /**
   * The shipped TOML the stack serves: `sep1.toml.value` names it (a container path in the default
   * profile, a repo path in CI), and the file of that name lives in `service-runner`'s config.
   */
  private fun servedFile(): Path {
    val name = Path.of(config.env["sep1.toml.value"]!!).fileName.toString()
    var dir: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath()
    while (dir != null) {
      val candidate = dir.resolve("service-runner/src/main/resources/config/$name")
      if (Files.exists(candidate)) return candidate
      dir = dir.parent
    }
    fail<Unit>(
      "no $name under service-runner/src/main/resources/config above ${System.getProperty("user.dir")}"
    )
    throw IllegalStateException()
  }

  // SEP1SV-01, SEP1SV-02, SEP1SV-03, SEP1SV-04
  @ParameterizedTest
  @ValueSource(strings = ["*/*", "text/plain", "application/toml", "application/json"])
  fun `test the TOML is served as text plain for every accepted media type`(accept: String) {
    val response = withClient { it.get(tomlUrl) { header(HttpHeaders.Accept, accept) } }

    assertEquals(HttpStatusCode.OK, response.status)
    assertTrue(response.contentType()!!.match(ContentType.Text.Plain)) {
      "Content-Type was ${response.contentType()}"
    }
  }

  // SEP1SV-05
  @Test
  fun `test the served TOML is the configured file byte for byte`() {
    val served = withClient { it.get(tomlUrl).readBytes() }

    assertArrayEquals(Files.readAllBytes(servedFile()), served)
  }

  // SEP1SV-06
  @Test
  fun `test the served TOML parses and is under 100000 bytes`() {
    val body = withClient { it.get(tomlUrl).readBytes() }

    assertTrue(body.size < 100_000) { "the TOML is ${body.size} bytes" }
    assertNotNull(Toml().read(String(body, Charsets.UTF_8)).getString("NETWORK_PASSPHRASE"))
  }

  // SEP1SV-07
  @Test
  fun `test GET answers with Access-Control-Allow-Origin star`() {
    val response = withClient {
      it.get(tomlUrl) { header(HttpHeaders.Origin, "https://wallet.example") }
    }

    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("*", response.headers[HttpHeaders.AccessControlAllowOrigin])
  }

  // SEP1SV-08
  @Test
  fun `test the preflight allows GET from any origin`() {
    val response = withClient {
      it.options(tomlUrl) {
        header(HttpHeaders.Origin, "https://wallet.example")
        header(HttpHeaders.AccessControlRequestMethod, "GET")
      }
    }

    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("*", response.headers[HttpHeaders.AccessControlAllowOrigin])
    assertTrue(response.headers[HttpHeaders.AccessControlAllowMethods]!!.contains("GET")) {
      "Access-Control-Allow-Methods was ${response.headers[HttpHeaders.AccessControlAllowMethods]}"
    }
  }

  // SEP1SV-09
  @Test
  fun `test HEAD answers with the headers of GET`() {
    val (get, head) = withClient { Pair(it.get(tomlUrl), it.head(tomlUrl)) }

    assertEquals(HttpStatusCode.OK, head.status)
    assertEquals(get.contentType(), head.contentType())
    assertEquals(get.contentLength(), head.contentLength())
  }

  // SEP1SV-10
  @Test
  fun `test the landing page redirects to the TOML`() {
    val response = withClient(followRedirects = false) { it.get("$domain/") }

    assertEquals(HttpStatusCode.Found, response.status)
    assertTrue(response.headers[HttpHeaders.Location]!!.endsWith("/.well-known/stellar.toml")) {
      "Location was ${response.headers[HttpHeaders.Location]}"
    }
  }
}
