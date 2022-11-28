package org.stellar.anchor.platform.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.validation.Errors

fun assertErrorCode(errors: Errors, code: String) {
  assertTrue(errors.hasErrors())
  assertEquals(code, errors.allErrors[0].code)
}
