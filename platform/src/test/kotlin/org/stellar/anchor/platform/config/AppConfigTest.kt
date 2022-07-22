package org.stellar.anchor.platform.config

import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.fail
import org.junit.jupiter.api.*
import org.springframework.validation.BindException
import org.springframework.validation.ValidationUtils

open class AppConfigTest {
  @Test
  fun testAppConfigValid() {
    val appConfig = PropertyAppConfig()
    appConfig.setJwtSecretKey("secret")
    appConfig.setAssets("test_assets.json")
    appConfig.setHorizonUrl("https://horizon-testnet.stellar.org")
    appConfig.setStellarNetworkPassphrase("Test SDF Network ; September 2015")
    appConfig.setHostUrl("localhost:8080")
    val errors = BindException(appConfig, "appConfig")
    ValidationUtils.invokeValidator(appConfig, appConfig, errors)
    if (errors.hasErrors()) {
      errors.printStackTrace()
      fail("found validation error(s)")
    }
  }

  @Test
  fun testAppConfigMissingSecret() {
    val appConfig = PropertyAppConfig()
    appConfig.setAssets("test_assets.json")
    appConfig.setHorizonUrl("https://horizon-testnet.stellar.org")
    appConfig.setStellarNetworkPassphrase("Test SDF Network ; September 2015")
    appConfig.setHostUrl("localhost:8080")
    val errors = BindException(appConfig, "appConfig")
    ValidationUtils.invokeValidator(appConfig, appConfig, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "empty-jwtSecretKey") }
  }

  @Test
  fun testAppConfigBadAsset() {
    val appConfig = PropertyAppConfig()
    appConfig.setJwtSecretKey("secret")
    appConfig.setAssets("test_assets_does_not_exist.json")
    appConfig.setHorizonUrl("https://horizon-testnet.stellar.org")
    appConfig.setStellarNetworkPassphrase("Test SDF Network ; September 2015")
    appConfig.setHostUrl("localhost:8080")
    val errors = BindException(appConfig, "appConfig")
    ValidationUtils.invokeValidator(appConfig, appConfig, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "doesNotExist-assets") }
  }
}
