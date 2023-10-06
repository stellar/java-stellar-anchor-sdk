@file:Suppress("unused")

package org.stellar.anchor.sep1

import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.config.Sep1Config
import org.stellar.anchor.config.Sep1Config.TomlType.*

@Order(89)
internal class Sep1ServiceTest {
  @MockK(relaxed = true) lateinit var sep1Config: Sep1Config

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this)
  }

  @Test
  fun `disabled Sep1Service should throw SepException when reading the toml value`() {
    // Given
    every { sep1Config.isEnabled } returns false
    // When
    val sep1Service = Sep1Service(sep1Config)
    // Then
    assertThrows<SepException> { sep1Service.toml }
  }

  @Test
  fun `test string type`() {
    // Given
    every { sep1Config.isEnabled } returns true
    every { sep1Config.type } returns STRING
    every { sep1Config.value } returns "toml content"
    // When
    val sep1Service = Sep1Service(sep1Config)
    // Then
    assertEquals("toml content", sep1Service.toml)
  }

  @Test
  fun `test file type`() {
    // Given
    every { sep1Config.isEnabled } returns true
    every { sep1Config.type } returns FILE
    every { sep1Config.value } returns "toml_file_path"
    val sep1Service = spyk(Sep1Service(sep1Config))
    every { sep1Service.readTomlFromFile(eq("toml_file_path")) } returns "toml content"
    // When, Then
    assertEquals("toml content", sep1Service.toml)
  }

  @Test
  fun `test url type`() {
    // Given
    every { sep1Config.isEnabled } returns true
    every { sep1Config.type } returns URL
    every { sep1Config.value } returns "toml_file_path"
    val sep1Service = spyk(Sep1Service(sep1Config))
    every { sep1Service.readTomlFromURL(eq("toml_file_path")) } returns "toml content"
    // When, Then
    assertEquals("toml content", sep1Service.toml)
  }
}
