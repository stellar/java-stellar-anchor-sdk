package org.stellar.anchor.api.sep.sep10

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ValidationRequestTest {
  companion object {
    const val TEST_TRANSACTION = "TEST TXN"
  }

  @Test
  fun `test creation of ValidationRequest`() {
    val vr = ValidationRequest.of(TEST_TRANSACTION)
    assertEquals(TEST_TRANSACTION, vr.transaction)
  }
}
