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
    appConfig.jwtSecretKey = "secret"
    appConfig.assets = "test_assets.json"
    appConfig.horizonUrl = "https://horizon-testnet.stellar.org"
    appConfig.stellarNetworkPassphrase = "Test SDF Network ; September 2015"
    appConfig.hostUrl = "http://localhost:8080"
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
    appConfig.assets = "test_assets.json"
    appConfig.horizonUrl = "https://horizon-testnet.stellar.org"
    appConfig.stellarNetworkPassphrase = "Test SDF Network ; September 2015"
    appConfig.hostUrl = "http://localhost:8080"
    val errors = BindException(appConfig, "appConfig")
    ValidationUtils.invokeValidator(appConfig, appConfig, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "empty-jwtSecretKey") }
  }

  @Test
  fun testAppConfigBadAsset() {
    val appConfig = PropertyAppConfig()
    appConfig.jwtSecretKey = "secret"
    appConfig.assets = "test_assets_does_not_exist.json"
    appConfig.horizonUrl = "https://horizon-testnet.stellar.org"
    appConfig.stellarNetworkPassphrase = "Test SDF Network ; September 2015"
    appConfig.hostUrl = "http://localhost:8080"
    val errors = BindException(appConfig, "appConfig")
    ValidationUtils.invokeValidator(appConfig, appConfig, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "doesNotExist-assets") }
  }

  @Test
  fun testAppConfigBadHorizonUrl() {
    val appConfig = PropertyAppConfig()
    appConfig.jwtSecretKey = "secret"
    appConfig.assets = "test_assets.json"
    appConfig.horizonUrl = "not-a-url"
    appConfig.stellarNetworkPassphrase = "Test SDF Network ; September 2015"
    appConfig.hostUrl = "http://localhost:2222"
    val errors = BindException(appConfig, "appConfig")
    ValidationUtils.invokeValidator(appConfig, appConfig, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "invalidUrl-horizonUrl") }
  }
}
