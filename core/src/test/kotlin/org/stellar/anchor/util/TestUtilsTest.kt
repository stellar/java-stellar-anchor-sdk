package org.stellar.anchor.util

import java.io.IOException
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.util.TestUtils.callPrivate

class TestUtilsTest {
  class FooBar() {
    fun publicMethod() {
      println("publicMethod")
    }

    private fun privateMethod() {
      println("privateMethod")
    }

    private fun privateMethodWithArgs(foo: String) {
      println("privateMethodWithArgs: (foo: $foo)")
    }

    private fun privateMethodWithArgsAndResponse(foo: String): String {
      println("privateMethodWithArgsAndResponse: (foo: $foo), returns: bar")
      return "bar"
    }

    @Throws(IOException::class)
    private fun privateThrowingMethod() {
      println("privateThrowingMethod")
      throw IOException("privateThrowingMethod")
    }
  }

  private val fooBarInstance = FooBar()

  @Test
  fun test_callPrivate_onPublicMethod() {
    assertDoesNotThrow { callPrivate(fooBarInstance, "publicMethod") }
  }

  @Test
  fun test_callPrivate_onPrivateMethod() {
    assertDoesNotThrow { callPrivate(fooBarInstance, "privateMethod") }
  }

  @Test
  fun test_callPrivate_onPrivateMethod_withArguments() {
    assertDoesNotThrow { callPrivate(fooBarInstance, "privateMethodWithArgs", "foo") }
  }

  @Test
  fun test_callPrivate_onPrivateMethod_withArgumentsAndResponse() {
    var response: String? = null
    assertDoesNotThrow {
      response = callPrivate(fooBarInstance, "privateMethodWithArgsAndResponse", "foo") as String
    }
    assertEquals("bar", response)
  }

  @Test
  fun test_callPrivate_notFound() {
    val ex: Exception = assertThrows { callPrivate(fooBarInstance, "notFoundMethod", "foo") }
    assertInstanceOf(IllegalAccessException::class.java, ex)
    assertEquals("Method notFoundMethod was not found", ex.message)
  }

  @Test
  fun test_callPrivate_throwingException() {
    val ex: Exception = assertThrows { callPrivate(fooBarInstance, "privateThrowingMethod") }
    assertInstanceOf(IOException::class.java, ex)
    assertEquals("privateThrowingMethod", ex.message)
  }
}
