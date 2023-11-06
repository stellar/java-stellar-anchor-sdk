package org.stellar.anchor.client.configurator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.api.exception.InvalidConfigException
import org.stellar.anchor.platform.configurator.ConfigMap
import org.stellar.anchor.platform.configurator.ConfigMap.ConfigEntry

class ConfigMapTest {
  @Test
  fun `test getInt() ok`() {
    val testKey = "testKey"
    val cm = ConfigMap()
    cm.put(testKey, "1", ConfigMap.ConfigSource.ENV)
    Assertions.assertEquals(1, cm.getInt(testKey))
    cm.put(testKey, "-1", ConfigMap.ConfigSource.ENV)
    Assertions.assertEquals(-1, cm.getInt(testKey))
    cm.put(testKey, "0", ConfigMap.ConfigSource.ENV)
    Assertions.assertEquals(0, cm.getInt(testKey))
    cm.put(testKey, Integer.MAX_VALUE.toString(), ConfigMap.ConfigSource.ENV)
    Assertions.assertEquals(Integer.MAX_VALUE, cm.getInt(testKey))
    cm.put(testKey, Integer.MIN_VALUE.toString(), ConfigMap.ConfigSource.ENV)
    Assertions.assertEquals(Integer.MIN_VALUE, cm.getInt(testKey))
  }

  @ParameterizedTest
  @ValueSource(strings = ["1.0", "-1.0", "abc", ""])
  fun `test getInt() failure`(value: String) {
    val testKey = "testKey"
    val cm = ConfigMap()
    cm.put(testKey, value, ConfigMap.ConfigSource.ENV)
    assertThrows<InvalidConfigException> { cm.getInt(testKey) }
  }

  @ParameterizedTest
  @ValueSource(strings = ["true", "True"])
  fun `test getBoolean() true`(value: String) {
    val testKey = "testKey"
    val cm = ConfigMap()
    cm.put(testKey, value, ConfigMap.ConfigSource.ENV)
    Assertions.assertEquals(true, cm.getBoolean(testKey))
  }

  @ParameterizedTest
  @ValueSource(strings = ["0", "tr ue", "fa lse", "False", "false", ""])
  fun `test getBoolean() false`(value: String) {
    val testKey = "testKey"
    val cm = ConfigMap()
    cm.put(testKey, value, ConfigMap.ConfigSource.ENV)
    Assertions.assertEquals(false, cm.getBoolean(testKey))
  }

  @Test
  fun `test merge`() {
    val cm = ConfigMap()
    cm.put("clients", "", ConfigMap.ConfigSource.DEFAULT)
    cm.put("my.property", "", ConfigMap.ConfigSource.DEFAULT)
    cm.put("other", "", ConfigMap.ConfigSource.DEFAULT)
    val override = ConfigMap()
    override.put("clients[0]", "a", ConfigMap.ConfigSource.ENV)
    override.put("clients[1]", "b", ConfigMap.ConfigSource.ENV)
    override.put("my.property[0]", "c", ConfigMap.ConfigSource.ENV)

    cm.merge(override)

    assert(
      cm.data.equals(
        mapOf(
          "clients[0]" to ConfigEntry("a", ConfigMap.ConfigSource.ENV),
          "clients[1]" to ConfigEntry("b", ConfigMap.ConfigSource.ENV),
          "my.property[0]" to ConfigEntry("c", ConfigMap.ConfigSource.ENV),
          "other" to ConfigEntry("", ConfigMap.ConfigSource.DEFAULT)
        )
      )
    )
  }
}
