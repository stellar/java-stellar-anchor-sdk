package org.stellar.anchor.platform.config

import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.validation.BindException
import org.springframework.validation.Errors
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.config.AssetsConfig.AssetConfigType.*
import org.stellar.anchor.util.FileUtil

class AssetsConfigTest {
  private lateinit var config: PropertyAssetsConfig
  private lateinit var errors: Errors

  @BeforeEach
  fun setUp() {
    config = PropertyAssetsConfig()
    errors = BindException(config, "config")
  }

  @Test
  fun `Test good assets configuration files`() {
    config.type = JSON
    config.value = FileUtil.getResourceFileAsString("test_assets.json")
    config.validate(config, errors)
    assertFalse(errors.hasErrors())

    config.type = YAML
    config.value = FileUtil.getResourceFileAsString("test_assets.yaml")
    config.validate(config, errors)
    assertFalse(errors.hasErrors())
  }

  @Test
  fun `Test missing configuration file`() {
    config.type = FILE
    config.value = "file_missing.yaml"
    config.validate(config, errors)
    assertTrue(errors.hasErrors())
    assertErrorCode(errors, "assets-file-not-valid")
  }

  @ParameterizedTest
  @ValueSource(strings = ["/test_assets_mal_format.yaml", "/test_assets_mal_format.json"])
  fun `Test mal-formatted configuration file`(resourceFile: String) {
    config.type = FILE
    val resource = DefaultAssetService::class.java.getResource(resourceFile)
    config.value = Paths.get(resource!!.toURI()).toFile().absolutePath
    config.validate(config, errors)
    assertTrue(errors.hasErrors())
    assertErrorCode(errors, "assets-file-not-valid")
  }
}
