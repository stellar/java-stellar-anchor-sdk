package org.stellar.anchor.sep1

import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.stellar.anchor.config.Sep1Config

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class Sep1ServiceTest {
  @Test
  fun `simple test if the toml file is read`() {
    val sep1Config = mockk<Sep1Config>()
  }
}
