package org.stellar.anchor.plugins.asset

import com.google.gson.JsonSyntaxException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.Constants.Companion.TEST_ASSET
import org.stellar.anchor.Constants.Companion.TEST_ASSET_ISSUER_ACCOUNT_ID
import org.stellar.anchor.exception.SepNotFoundException

internal class ResourceJsonAssetServiceTest {
  @Test
  fun getListAllAssets() {
    val rjas = ResourceJsonAssetService("test_assets.json")
    assertEquals(rjas.assets.getAssets().size, 2)

    val assets = rjas.listAllAssets()
    assertTrue(assets.size == 2)

    val asset = rjas.getAsset(TEST_ASSET, TEST_ASSET_ISSUER_ACCOUNT_ID)
    assertEquals(asset.code, TEST_ASSET)
    assertEquals(asset.issuer, TEST_ASSET_ISSUER_ACCOUNT_ID)

    assertEquals(rjas.getAsset(TEST_ASSET, null).code, TEST_ASSET)

    assertNull(rjas.getAsset("NA", null))
  }

  @Test
  fun testJsonNotFound() {
    assertThrows<JsonSyntaxException> { ResourceJsonAssetService("test_assets.json.bad") }

    assertThrows<SepNotFoundException> { ResourceJsonAssetService("not_found.json") }
  }
}
