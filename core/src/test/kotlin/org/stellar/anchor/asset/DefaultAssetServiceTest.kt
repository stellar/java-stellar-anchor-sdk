package org.stellar.anchor.asset

import com.google.gson.JsonSyntaxException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.TestConstants.Companion.TEST_ASSET
import org.stellar.anchor.TestConstants.Companion.TEST_ASSET_ISSUER_ACCOUNT_ID
import org.stellar.anchor.api.exception.SepNotFoundException
import shadow.org.apache.commons.io.FilenameUtils

internal class DefaultAssetServiceTest {
  @ParameterizedTest
  @ValueSource(strings = ["test_assets.json", "test_assets.yaml"])
  fun `test assets listing`(filename: String) {

    lateinit var das: DefaultAssetService
    when (FilenameUtils.getExtension(filename)) {
      "json" -> das = DefaultAssetService.fromJsonResource("test_assets.json")
      "yaml" -> das = DefaultAssetService.fromYamlResource("test_assets.yaml")
    }

    assertEquals(3, das.assets.getAssets().size)
    val assets = das.listAllAssets()
    assertEquals(3, assets.size)
    val asset = das.getAsset("USDC", "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP")
    assertEquals(asset.code, TEST_ASSET)
    assertEquals(asset.issuer, TEST_ASSET_ISSUER_ACCOUNT_ID)
    assertEquals(das.getAsset(TEST_ASSET).code, TEST_ASSET)
    assertNull(das.getAsset("NA"))
  }

  @Test
  fun `test asset JSON file not found`() {
    assertThrows<JsonSyntaxException> {
      DefaultAssetService.fromJsonResource("test_assets.json.bad")
    }

    assertThrows<SepNotFoundException> { DefaultAssetService.fromJsonResource("not_found.json") }

    assertThrows<SepNotFoundException> {
      DefaultAssetService.fromJsonResource("classpath:/test_assets.json")
    }
  }
}
