package org.stellar.anchor.platform.integrationtest

import com.moandjiezana.toml.Toml
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.platform.Sep1TomlRules

/**
 * The test-side SEP-1 TOML validator, proven with a TOML that follows every rule and one TOML per
 * rule that breaks only that rule, so a rule that stops checking fails here and not silently on the
 * real TOMLs.
 */
class Sep1TomlRulesTests {
  private val host = "anchor.example.com"
  private val signingKey = "GCHLHDBOKG2JWMJQBTLSL5XG6NO7ESXI2TAQKZXCXWXB5WI2X6W233PR"
  private val issuer = "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"

  private val good =
    """
    VERSION = "0.1.0"
    SIGNING_KEY = "$signingKey"
    NETWORK_PASSPHRASE = "Test SDF Network ; September 2015"

    WEB_AUTH_ENDPOINT = "https://$host/auth"
    WEB_AUTH_FOR_CONTRACTS_ENDPOINT = "https://$host/sep45/auth"
    KYC_SERVER = "https://$host/sep12"
    TRANSFER_SERVER = "https://$host/sep6"
    TRANSFER_SERVER_SEP0024 = "https://$host/sep24"
    DIRECT_PAYMENT_SERVER = "https://$host/sep31"
    ANCHOR_QUOTE_SERVER = "https://$host/sep38"

    [[CURRENCIES]]
    code = "USDC"
    issuer = "$issuer"
    status = "test"
    is_asset_anchored = false
    desc = "Anchor Issued USDC Token"
    anchor_asset_type = "crypto"

    [[CURRENCIES]]
    code = "native"
    status = "test"
    is_asset_anchored = false
    anchor_asset_type = "crypto"
    desc = "XLM, the native token of the Stellar network."

    [DOCUMENTATION]
    ORG_URL = "https://$host"
    ORG_LOGO = "https://$host/logo.png"
    ORG_PHYSICAL_ADDRESS_ATTESTATION = "https://$host/address.pdf"
    """
      .trimIndent()

  private fun violations(toml: String, servedFrom: String = host) =
    Sep1TomlRules.violations(toml, servedFrom)

  private fun TomlCase(name: String, expected: String, toml: String) = Triple(name, expected, toml)

  // SEP1TF-10
  @Test
  fun `test a TOML that follows every rule has no violations`() {
    assertEquals(emptyList<String>(), violations(good))
  }

  // SEP1TF-10
  @Test
  fun `test each rule reports exactly its own violation`() {
    val cases =
      listOf(
        TomlCase("not a TOML", "NOT_TOML", "this = is = not toml"),
        TomlCase("too large", "SIZE", good + "\n" + "#".repeat(100_000)),
        TomlCase(
          "passphrase",
          "NETWORK_PASSPHRASE",
          good.replace("Test SDF Network ; September 2015", "foo"),
        ),
        TomlCase("signing key", "SIGNING_KEY", good.replace(signingKey, "GABC")),
        TomlCase(
          "code too long",
          "CURRENCY[0]:code",
          good.replace("\"USDC\"", "\"ABCDEFGHIJKLM\"")
        ),
        TomlCase(
          "desc missing",
          "CURRENCY[0]:desc",
          good.replace("desc = \"Anchor Issued USDC Token\"\n", ""),
        ),
        TomlCase(
          "status",
          "CURRENCY[0]:status",
          good.replaceFirst("status = \"test\"", "status = \"foo\"")
        ),
        TomlCase(
          "is_asset_anchored not boolean",
          "CURRENCY[0]:is_asset_anchored",
          good.replaceFirst("is_asset_anchored = false", "is_asset_anchored = \"yes\""),
        ),
        TomlCase(
          "anchor_asset_type",
          "CURRENCY[0]:anchor_asset_type",
          good.replaceFirst("anchor_asset_type = \"crypto\"", "anchor_asset_type = \"foo\""),
        ),
        TomlCase("issuer", "CURRENCY[0]:issuer", good.replace(issuer, "GABC")),
        TomlCase(
          "linked TOML with another field",
          "LINKED_TOML[2]:EXTRA_FIELDS",
          good + "\n[[CURRENCIES]]\ntoml = \"https://$host/USDC.toml\"\ncode = \"USDC\"\n",
        ),
        TomlCase(
          "linked TOML over http",
          "LINKED_TOML[2]:URL",
          good + "\n[[CURRENCIES]]\ntoml = \"http://$host/USDC.toml\"\n",
        ),
        TomlCase(
          "top-level URL over http",
          "NOT_HTTPS:KYC_SERVER",
          good.replace("https://$host/sep12", "http://$host/sep12"),
        ),
        TomlCase(
          "documentation URL over http",
          "NOT_HTTPS:ORG_LOGO",
          good.replace("https://$host/logo.png", "http://$host/logo.png"),
        ),
        TomlCase(
          "currency URL over http",
          "NOT_HTTPS:CURRENCIES[0].image",
          good.replaceFirst(
            "anchor_asset_type = \"crypto\"",
            "anchor_asset_type = \"crypto\"\nimage = \"http://$host/usdc.png\"",
          ),
        ),
        TomlCase(
          "trailing slash",
          "TRAILING_SLASH:KYC_SERVER",
          good.replace("https://$host/sep12", "https://$host/sep12/"),
        ),
        TomlCase(
          "ORG_URL on another domain",
          "ORG_URL_DOMAIN",
          good
            .replace("ORG_URL = \"https://$host\"", "ORG_URL = \"https://other.example.org\"")
            .replace("\nORG_PHYSICAL_ADDRESS_ATTESTATION = \"https://$host/address.pdf\"", ""),
        ),
        TomlCase(
          "attestation on another domain",
          "ATTESTATION_DOMAIN:ORG_PHYSICAL_ADDRESS_ATTESTATION",
          good.replace("https://$host/address.pdf", "https://other.example.org/address.pdf"),
        ),
      )

    cases.forEach { (name, expected, toml) ->
      assertEquals(listOf(expected), violations(toml), "case: $name")
    }
  }

  // SEP1TF-06, SEP1TF-08
  @ParameterizedTest
  @ValueSource(strings = ["localhost", "127.0.0.1", "host.docker.internal"])
  fun `test a local development host may use http and any ORG_URL`(local: String) {
    val http = good.replace("https://$host", "http://$local:8080")
    val otherOrgUrl =
      http.replace("ORG_URL = \"http://$local:8080\"", "ORG_URL = \"https://stellar.org\"")

    assertEquals(emptyList<String>(), violations(http, local))
    assertEquals(emptyList<String>(), violations(otherOrgUrl, local))
  }

  // SEP1TF-06
  @Test
  fun `test http URLs are refused on a host that is not a local development host`() {
    val http = good.replace("https://$host/sep", "http://$host/sep")

    assertEquals(
      listOf(
          "NOT_HTTPS:KYC_SERVER",
          "NOT_HTTPS:WEB_AUTH_FOR_CONTRACTS_ENDPOINT",
          "NOT_HTTPS:TRANSFER_SERVER",
          "NOT_HTTPS:TRANSFER_SERVER_SEP0024",
          "NOT_HTTPS:DIRECT_PAYMENT_SERVER",
          "NOT_HTTPS:ANCHOR_QUOTE_SERVER",
        )
        .sorted(),
      violations(http).sorted(),
    )
  }

  // SEP1TF-04: edge cases
  @Test
  fun `test the native currency, nft and a TOML without documentation are accepted`() {
    val nft = good.replaceFirst("anchor_asset_type = \"crypto\"", "anchor_asset_type = \"nft\"")
    val withoutDocumentation = good.substringBefore("[DOCUMENTATION]").trimEnd()

    assertEquals(emptyList<String>(), violations(nft))
    assertEquals(emptyList<String>(), violations(withoutDocumentation))
  }

  private fun repoFile(relative: String): Path {
    var dir: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath()
    while (dir != null) {
      if (Files.exists(dir.resolve(relative))) return dir.resolve(relative)
      dir = dir.parent
    }
    fail<Unit>("$relative not found above ${System.getProperty("user.dir")}")
    throw IllegalStateException()
  }

  // SEP1TF-11: each template is checked against the host its own WEB_AUTH_ENDPOINT names.
  @ParameterizedTest
  @ValueSource(
    strings =
      [
        "service-runner/src/main/resources/config/stellar.localhost.toml",
        "service-runner/src/main/resources/config/stellar.host.docker.internal.toml",
        "docs/resources/deployment-examples/example-fargate/example_config/stellar.toml",
      ]
  )
  fun `test a TOML shipped for operators follows every rule`(relative: String) {
    val toml = Files.readString(repoFile(relative))
    val servedFrom = URI(Toml().read(toml).getString("WEB_AUTH_ENDPOINT")).host

    assertEquals(emptyList<String>(), violations(toml, servedFrom), relative)
  }
}
