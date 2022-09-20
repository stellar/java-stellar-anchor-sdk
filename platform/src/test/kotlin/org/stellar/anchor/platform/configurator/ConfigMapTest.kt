package org.stellar.anchor.platform.configurator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.api.exception.InvalidConfigException

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
}
