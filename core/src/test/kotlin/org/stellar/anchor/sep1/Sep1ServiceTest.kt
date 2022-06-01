package org.stellar.anchor.sep1

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.stellar.anchor.config.Sep1Config

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class Sep1ServiceTest {
  @Test
  fun testToml() {
    val sep1Config = mockk<Sep1Config>()
    every { sep1Config.stellarFile } returns "test_stellar.toml"

    val sep1Service = Sep1Service(sep1Config)

    assertNotNull(sep1Service.stellarToml)
    verify(exactly = 3) { sep1Config.stellarFile }
  }
}
