package org.stellar.anchor.platform.configurator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class ConfigHelperTest {
  @ParameterizedTest
  @CsvSource(
    value =
      [
        "clients[0]_name,clients,[0],name",
        "clients[1]_name,clients,[1],name",
        "clients[2]_name,clients,[2],name",
        "clients[0]_type,clients,[0],type",
        "clients[0]_callback_url,clients,[0],callback_url"
      ]
  )
  fun `test the list name extraction if the name is a list`(
    name: String,
    listName: String,
    index: String,
    elementName: String
  ) {
    val result = ConfigHelper.extractListNameIfAny(name)
    assertEquals(result.listName, listName)
    assertEquals(result.elementName, elementName)
  }

  @ParameterizedTest
  @ValueSource(strings = ["clients_name", "clients[0_name"])
  fun `test the list name extraction if the name is not a list`(name: String) {
    val result = ConfigHelper.extractListNameIfAny(name)
    assertNull(result)
  }
}
