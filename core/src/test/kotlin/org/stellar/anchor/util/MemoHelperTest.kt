package org.stellar.anchor.util

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.exception.SepValidationException
import org.stellar.sdk.xdr.MemoType

internal class MemoHelperTest {
  @Test
  fun `Test makeMemo error`() {
    assertThrows<SepValidationException> { MemoHelper.makeMemo("memo", "bad_type") }

    assertThrows<SepValidationException> { MemoHelper.makeMemo("bad_number", "id") }

    assertThrows<IllegalArgumentException> { MemoHelper.makeMemo("bad_hash", "hash") }

    assertThrows<SepException> { MemoHelper.makeMemo("none", "none") }

    assertThrows<SepException> { MemoHelper.makeMemo("return", "return") }

    assertThrows<SepException> { MemoHelper.makeMemo("none", MemoType.MEMO_NONE) }

    assertThrows<SepException> { MemoHelper.makeMemo("return", MemoType.MEMO_RETURN) }
  }

  @Test
  fun `test memo hash conversion`() {
    val wantHex = "39623738663066612d393366392d343139382d386439332d6537366664303834"
    val gotHex = MemoHelper.convertBase64ToHex("OWI3OGYwZmEtOTNmOS00MTk4LThkOTMtZTc2ZmQwODQ=")
    assertEquals(wantHex, gotHex)

    val wantBase64 = "OWI3OGYwZmEtOTNmOS00MTk4LThkOTMtZTc2ZmQwODQ="
    val gotBase64 =
      MemoHelper.convertHexToBase64(
        "39623738663066612d393366392d343139382d386439332d6537366664303834"
      )
    assertEquals(wantBase64, gotBase64)
  }
}
