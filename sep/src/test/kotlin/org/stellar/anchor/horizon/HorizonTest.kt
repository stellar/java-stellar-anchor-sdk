package org.stellar.anchor.horizon

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.stellar.anchor.config.AppConfig

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HorizonTest {
  companion object {
    const val TEST_HORIZON_URI = "https://horizon-testnet.stellar.org/"
  }

  @Test
  fun testHorizon() {
    val appConfig = mockk<AppConfig>()
    every { appConfig.horizonUrl } returns TEST_HORIZON_URI

    val horizonServer = Horizon(appConfig)

    assertNotNull(horizonServer.server)
  }
}
