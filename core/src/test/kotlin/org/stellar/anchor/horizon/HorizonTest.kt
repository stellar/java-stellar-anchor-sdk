package org.stellar.anchor.horizon

import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.stellar.anchor.config.AppConfig

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HorizonTest {
  companion object {
    const val TEST_HORIZON_URI = "https://horizon-testnet.stellar.org/"
    const val TEST_HORIZON_PASSPHRASE = "Test SDF Network ; September 2015"
  }

  @Test
  fun `test the correctness of Horizon creation`() {
    val appConfig = mockk<AppConfig>()
    every { appConfig.horizonUrl } returns TEST_HORIZON_URI
    every { appConfig.stellarNetworkPassphrase } returns TEST_HORIZON_PASSPHRASE

    val horizonServer = Horizon(appConfig)

    assertNotNull(horizonServer.server)
    assertEquals(TEST_HORIZON_URI, horizonServer.horizonUrl)
    assertEquals(TEST_HORIZON_PASSPHRASE, horizonServer.stellarNetworkPassphrase)
  }
}
