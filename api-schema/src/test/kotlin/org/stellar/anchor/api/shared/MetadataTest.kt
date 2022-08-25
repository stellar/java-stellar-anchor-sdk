package org.stellar.anchor.api.shared

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MetadataTest {
  @Test
  fun `test Metadata#getVersion`() {
    Assertions.assertEquals("0.1.2", Metadata.getVersion())
  }
}
