package org.stellar.anchor.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.stellar.anchor.util.PropertyUtil.set;

import java.util.Optional;
import lombok.Data;
import org.junit.jupiter.api.Test;

class PropertyUtilTest {
  @Data
  public static class A {
    String value;
    B b;
  }

  @Data
  public static class B {
    String value;
    C c;
  }

  @Data
  public static class C {
    String value;
  }

  @Test
  void testSet() throws ReflectiveOperationException {
    A a = new A();
    assertEquals(Optional.empty(), PropertyUtil.get(a, "value"));
    set(a, "value", "value-a");
    assertEquals("value-a", PropertyUtil.get(a, "value").orElse(null));

    assertEquals(Optional.empty(), PropertyUtil.get(a, "b.value"));
    set(a, "b.value", "value-b");
    assertEquals("value-b", PropertyUtil.get(a, "b.value").orElse(null));

    assertEquals(Optional.empty(), PropertyUtil.get(a, "b.c.value"));
    set(a, "b.c.value", "value-c");
    assertEquals("value-c", PropertyUtil.get(a, "b.c.value").orElse(null));
  }

  @Test
  void testNotExists() throws ReflectiveOperationException {
    A a = new A();
    assertEquals(Optional.empty(), PropertyUtil.get(a, "value"));
    assertEquals(Optional.empty(), PropertyUtil.get(a, "b"));
    assertEquals(Optional.empty(), PropertyUtil.get(a, "b.c"));
    assertEquals(Optional.empty(), PropertyUtil.get(a, "not_found"));
  }
}
