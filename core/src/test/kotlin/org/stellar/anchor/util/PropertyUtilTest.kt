package org.stellar.anchor.util

import java.util.*
import lombok.Data
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PropertyUtilTest {
  @Data
  class A {
    var value: String? = null
    var b: B? = null
  }

  @Data
  class B {
    var value: String? = null
    var c: C? = null
  }

  @Data
  class C {
    var value: String? = null
  }

  @Test
  fun `test set property`() {
    val a = A()
    assertEquals(Optional.empty<Any>(), PropertyUtil.get(a, "value"))
    PropertyUtil.set(a, "value", "value-a")
    assertEquals("value-a", PropertyUtil.get(a, "value").orElse(null))
    assertEquals(Optional.empty<Any>(), PropertyUtil.get(a, "b.value"))
    PropertyUtil.set(a, "b.value", "value-b")
    assertEquals("value-b", PropertyUtil.get(a, "b.value").orElse(null))
    assertEquals(Optional.empty<Any>(), PropertyUtil.get(a, "b.c.value"))
    PropertyUtil.set(a, "b.c.value", "value-c")
    assertEquals("value-c", PropertyUtil.get(a, "b.c.value").orElse(null))
  }

  @Test
  fun `test get non-existent property returns empty`() {
    val a = A()
    assertEquals(Optional.empty<Any>(), PropertyUtil.get(a, "value"))
    assertEquals(Optional.empty<Any>(), PropertyUtil.get(a, "b"))
    assertEquals(Optional.empty<Any>(), PropertyUtil.get(a, "b.c"))
    assertEquals(Optional.empty<Any>(), PropertyUtil.get(a, "not_found"))
  }
}
