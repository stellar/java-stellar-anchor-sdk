package org.stellar.anchor.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.util.StringHelper.*

class StringHelperTest {
  @Test
  fun `test string isEmpty()`() {
    assertTrue(isEmpty(null))
    assertTrue(isEmpty(""))
    assertFalse(isEmpty("-1"))
    assertFalse(isEmpty("not empty"))
  }

  @Test
  fun `test camelToSnake()`() {
    assertEquals("hello_world", camelToSnake("helloWorld"))
    assertEquals("hello_world", camelToSnake("HelloWorld"))
    assertEquals("hello_world", camelToSnake("hello_World"))
    assertEquals("hello_world", camelToSnake("hello_WORLD"))
    assertEquals("hello_world", camelToSnake("helloWORLD"))
    assertEquals("hello.world", camelToSnake("hello.world"))
    assertEquals("hello.world", camelToSnake("hello.World"))
    assertEquals("helloworld", camelToSnake("Helloworld"))

    assertEquals("hello_world.good_morning", camelToSnake("hello-world.goodMorning"))
    assertEquals("hello1.world", camelToSnake("hello1.World"))

    assertEquals("", camelToSnake(""))
    assertThrows<java.lang.NullPointerException> { camelToSnake(null) }
  }
}
