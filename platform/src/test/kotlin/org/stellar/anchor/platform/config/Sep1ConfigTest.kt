package org.stellar.anchor.platform.config

import java.net.URL
import java.nio.file.Paths
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.validation.BindException
import org.springframework.validation.ValidationUtils
import org.stellar.anchor.api.exception.InvalidConfigException
import org.stellar.anchor.config.Sep1Config.TomlType.FILE
import org.stellar.anchor.config.Sep1Config.TomlType.fromString
import org.stellar.anchor.platform.config.PropertySep1Config.TomlConfig

class Sep1ConfigTest {
  companion object {
    fun getTestTomlAsFile(): String {
      val resource: URL =
        Sep1ConfigTest::class
          .java
          .getResource("/org/stellar/anchor/platform/config/sep1-stellar-test.toml") as URL
      return Paths.get(resource.toURI()).toFile().absolutePath
    }

    fun getTestTomlAsUrl(): String {
      val resource: URL =
        Sep1ConfigTest::class
          .java
          .getResource("/org/stellar/anchor/platform/config/sep1-stellar-test.toml") as URL
      return resource.toString()
    }

    fun validate(config: PropertySep1Config): BindException {
      val errors = BindException(config, "sep1Config")
      ValidationUtils.invokeValidator(config, config, errors)
      return errors
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["file", "FILE", "File"])
  fun `test reading from sep1-stellar-test toml file`(type: String) {
    val errors =
      validate(PropertySep1Config(true, TomlConfig(fromString(type), getTestTomlAsFile())))
    assertEquals(0, errors.errorCount)
  }

  @ParameterizedTest
  @ValueSource(strings = ["string", "STRING", "String"])
  fun `test inline toml`(type: String) {
    val errors =
      validate(PropertySep1Config(true, TomlConfig(fromString(type), "VERSION = \"0.1.0\"")))
    assertEquals(0, errors.errorCount)
  }

  @ParameterizedTest
  @ValueSource(strings = ["url", "URL", "Url", "urL"])
  fun `test toml with sep1-stellar-test specified as a URL`(type: String) {
    val errors =
      validate(PropertySep1Config(true, TomlConfig(fromString(type), getTestTomlAsUrl())))
    assertEquals(0, errors.errorCount)
  }

  @ParameterizedTest
  @ValueSource(strings = ["bad", "strin g"])
  fun `test bad Sep1Config values`(type: String?) {
    assertThrows<InvalidConfigException> {
      validate(PropertySep1Config(true, TomlConfig(fromString(type), getTestTomlAsUrl())))
    }
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = [""])
  fun `test empty Sep1Config types`(type: String?) {
    assertThrows<InvalidConfigException> {
      validate(PropertySep1Config(true, TomlConfig(fromString(type), getTestTomlAsUrl())))
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["bad file", "c:/hello"])
  fun `test file of Sep1Config does not exist`(file: String) {
    var errors = validate(PropertySep1Config(true, TomlConfig(FILE, file)))
    assertEquals(1, errors.errorCount)
    assertEquals("sep1-toml-value-file-does-not-exist", errors.allErrors[0].code)

    errors = validate(PropertySep1Config(false, TomlConfig(FILE, file)))
    assertEquals(0, errors.errorCount)
  }

  @ParameterizedTest
  @ValueSource(strings = ["string", "file", "url"])
  fun `test Sep1Config empty values`(type: String) {
    var errors = validate(PropertySep1Config(true, TomlConfig(fromString(type), null)))
    assertEquals(1, errors.errorCount)
    assertEquals("sep1-toml-value-empty", errors.allErrors[0].code)

    errors = validate(PropertySep1Config(true, TomlConfig(fromString(type), "")))
    assertEquals(1, errors.errorCount)
    assertEquals("sep1-toml-value-empty", errors.allErrors[0].code)
  }
}
