package org.stellar.anchor.platform.config

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.stellar.anchor.config.AssetsConfig
import org.stellar.anchor.util.FileUtil

class PropertyAssetsConfigTest {
  private lateinit var config: PropertyAssetsConfig
  private lateinit var errors: Errors

  @BeforeEach
  fun setUp() {
    config = PropertyAssetsConfig()
    errors = BindException(config, "config")
  }

  @Test
  fun `Test good assets configuration files`() {
    config.type = AssetsConfig.AssetConfigType.JSON
    config.value = FileUtil.getResourceFileAsString("test_assets_duplicate_asset.json")
    config.validate(config, errors)
    Assertions.assertTrue(errors.hasErrors())
    assertErrorCode(errors, "invalid-asset-duplicate-exists")

    config.type = AssetsConfig.AssetConfigType.YAML
    config.value = FileUtil.getResourceFileAsString("test_assets_duplicate_asset.yaml")
    config.validate(config, errors)
    Assertions.assertTrue(errors.hasErrors())
    assertErrorCode(errors, "invalid-asset-duplicate-exists")
  }
}
