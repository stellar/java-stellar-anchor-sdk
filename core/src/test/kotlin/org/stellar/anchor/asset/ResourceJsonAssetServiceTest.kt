package org.stellar.anchor.asset

import com.google.gson.JsonSyntaxException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.Constants.Companion.TEST_ASSET
import org.stellar.anchor.Constants.Companion.TEST_ASSET_ISSUER_ACCOUNT_ID
import org.stellar.anchor.api.exception.SepNotFoundException

internal class ResourceJsonAssetServiceTest {
  @Test
  fun `test assets listing`() {
    val rjas = ResourceJsonAssetService("test_assets.json")
    assertEquals(3, rjas.assets.getAssets().size)

    val assets = rjas.listAllAssets()
    assertEquals(3, assets.size)

    val asset = rjas.getAsset(TEST_ASSET, TEST_ASSET_ISSUER_ACCOUNT_ID)
    assertEquals(asset.code, TEST_ASSET)
    assertEquals(asset.issuer, TEST_ASSET_ISSUER_ACCOUNT_ID)

    assertEquals(rjas.getAsset(TEST_ASSET).code, TEST_ASSET)

    assertNull(rjas.getAsset("NA"))
  }

  @Test
  fun `test asset JSON file not found`() {
    assertThrows<JsonSyntaxException> { ResourceJsonAssetService("test_assets.json.bad") }

    assertThrows<SepNotFoundException> { ResourceJsonAssetService("not_found.json") }

    assertThrows<SepNotFoundException> { ResourceJsonAssetService("classpath:/test_assets.json") }
  }
}
