package org.stellar.anchor.util

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.util.SepLanguageHelper.validateLanguage

class SepLanguageHelperTest {
  @MockK(relaxed = true) lateinit var appConfig: AppConfig

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    SepLanguageHelper.reset()
  }
  @Test
  fun `test validateLanguage()`() {
    every { appConfig.languages } returns
      listOf(
        "en",
        "es",
        "fr",
        "en-US",
        "en-CA",
        "es-ES",
        "fr-FR",
        "fr-CA",
        "zh-TW",
        "zh-CN",
        "uk-UA"
      )

    Assertions.assertEquals("en", validateLanguage(appConfig, "pt"))
    Assertions.assertEquals("en", validateLanguage(appConfig, "uk"))
    Assertions.assertEquals("en", validateLanguage(appConfig, "zh"))
    Assertions.assertEquals("en", validateLanguage(appConfig, "en"))
    Assertions.assertEquals("es", validateLanguage(appConfig, "es"))
    Assertions.assertEquals("fr", validateLanguage(appConfig, "fr"))

    Assertions.assertEquals("fr-FR", validateLanguage(appConfig, "fr-BE"))
    Assertions.assertEquals("fr-FR", validateLanguage(appConfig, "fr-FR"))
    Assertions.assertEquals("fr-CA", validateLanguage(appConfig, "fr-CA"))
    Assertions.assertEquals("zh-TW", validateLanguage(appConfig, "zh-HK"))
    Assertions.assertEquals("es-ES", validateLanguage(appConfig, "es-AR"))
    Assertions.assertEquals("es-ES", validateLanguage(appConfig, "es-BR"))
    Assertions.assertEquals("uk-UA", validateLanguage(appConfig, "uk-RU"))
    Assertions.assertEquals("en-US", validateLanguage(appConfig, "pt-BR"))
    Assertions.assertEquals("en-US", validateLanguage(appConfig, "en-UK"))
    Assertions.assertEquals("en-CA", validateLanguage(appConfig, "en-CA"))
    Assertions.assertEquals("en-US", validateLanguage(appConfig, "en-US"))

    Assertions.assertEquals("en-US", validateLanguage(appConfig, null))
    Assertions.assertEquals("en-US", validateLanguage(appConfig, "good-language"))
    Assertions.assertEquals("en", validateLanguage(appConfig, "bad language"))
  }
}
