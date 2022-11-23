@file:Suppress("unused")

package org.stellar.anchor.sep1

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.InvalidConfigException
import org.stellar.anchor.config.Sep1Config
import org.stellar.anchor.util.NetUtil

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class Sep1ServiceTest {

  @MockK(relaxed = true) lateinit var sep1Config: Sep1Config

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `disabled Sep1Service should return null string as toml value`() {
    every { sep1Config.isEnabled } returns false
    val sep1Service = Sep1Service(sep1Config)
    assertEquals(null, sep1Service.stellarToml)
  }

  @Test
  fun `test string type`() {
    every { sep1Config.isEnabled } returns true
    every { sep1Config.type } returns "string"
    every { sep1Config.value } returns "toml content"
    val sep1Service = Sep1Service(sep1Config)
    assertEquals("toml content", sep1Service.stellarToml)
  }

  @Test
  fun `test file type`() {
    mockkStatic(Files::class)
    every { Files.readString(any()) } returns "toml content"
    every { sep1Config.isEnabled } returns true
    every { sep1Config.type } returns "file"
    every { sep1Config.value } returns "toml_file_path"

    val sep1Service = Sep1Service(sep1Config)
    assertEquals("toml content", sep1Service.stellarToml)
  }

  @Test
  fun `test url type`() {
    mockkStatic(NetUtil::class)
    every { NetUtil.fetch(any()) } returns "toml content"
    every { sep1Config.isEnabled } returns true
    every { sep1Config.type } returns "url"
    every { sep1Config.value } returns "toml_file_path"

    val sep1Service = Sep1Service(sep1Config)
    assertEquals("toml content", sep1Service.stellarToml)
  }

  @Test
  fun `test bad type`() {
    every { sep1Config.isEnabled } returns true
    every { sep1Config.type } returns "bad"
    every { sep1Config.value } returns "toml_file_path"

    assertThrows<InvalidConfigException> { Sep1Service(sep1Config) }
  }
}
