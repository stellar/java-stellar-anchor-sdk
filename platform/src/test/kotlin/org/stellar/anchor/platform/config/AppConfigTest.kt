package org.stellar.anchor.platform.config

import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.fail
import org.junit.jupiter.api.Test
import org.springframework.validation.BindException
import org.springframework.validation.ValidationUtils

class AppConfigTest {
  @Test
  fun testAppConfigValid() {
    val appConfig = PropertyAppConfig()
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
  fun testAppConfigBadHorizonUrl() {
    val appConfig = PropertyAppConfig()
    appConfig.horizonUrl = "not-a-url"
    appConfig.stellarNetworkPassphrase = "Test SDF Network ; September 2015"
    appConfig.hostUrl = "http://localhost:2222"
    val errors = BindException(appConfig, "appConfig")
    ValidationUtils.invokeValidator(appConfig, appConfig, errors)
    assertEquals(1, errors.errorCount)
    errors.message?.let { assertContains(it, "invalidUrl-horizonUrl") }
  }
}
