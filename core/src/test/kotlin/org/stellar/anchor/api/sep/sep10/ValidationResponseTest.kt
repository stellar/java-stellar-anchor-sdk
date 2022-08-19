package org.stellar.anchor.api.sep.sep10

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ValidationResponseTest {
  companion object {
    const val TEST_TOKEN = "TEST TOKEN"
  }

  @Test
  fun `test creation of ValidationResponse`() {
    val vr = ValidationResponse.of(TEST_TOKEN)
    assertEquals(TEST_TOKEN, vr.token)
  }
}
