package org.stellar.anchor.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.exception.SepValidationException
import org.stellar.sdk.xdr.MemoType

internal class MemoHelperTest {
  @Test
  fun testMakeMemoError() {
    assertThrows<SepValidationException> { MemoHelper.makeMemo("memo", "bad_type") }

    assertThrows<SepValidationException> { MemoHelper.makeMemo("bad_number", "id") }

    assertThrows<IllegalArgumentException> { MemoHelper.makeMemo("bad_hash", "hash") }

    assertThrows<SepException> { MemoHelper.makeMemo("none", "none") }

    assertThrows<SepException> { MemoHelper.makeMemo("return", "return") }

    assertThrows<SepException> { MemoHelper.makeMemo("none", MemoType.MEMO_NONE) }

    assertThrows<SepException> { MemoHelper.makeMemo("return", MemoType.MEMO_RETURN) }
  }
}
