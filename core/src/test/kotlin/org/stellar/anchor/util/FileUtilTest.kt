package org.stellar.anchor.util

import org.junit.jupiter.api.Test

internal class FileUtilTest {
  @Test
  fun testGetResourceFileAsString() {
    val value = FileUtil.getResourceFileAsString("test_resource.txt")
    assert(value.equals("test_resource.txt"))
  }
}
