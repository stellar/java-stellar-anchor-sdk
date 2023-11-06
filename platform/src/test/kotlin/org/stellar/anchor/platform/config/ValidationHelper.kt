package org.stellar.anchor.client.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.validation.Errors

fun assertErrorCode(errors: Errors, code: String) {
  assertTrue(errors.hasErrors())
  assertEquals(code, errors.allErrors[0].code)
}
