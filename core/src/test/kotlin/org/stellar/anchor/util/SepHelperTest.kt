package org.stellar.anchor.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.sdk.xdr.MemoType

internal class SepHelperTest {
  @Test
  fun `test memoType conversion`() {
    assert(SepHelper.memoTypeString(MemoType.MEMO_ID).equals("id"))
    assert(SepHelper.memoTypeString(MemoType.MEMO_HASH).equals("hash"))
    assert(SepHelper.memoTypeString(MemoType.MEMO_TEXT).equals("text"))
    assert(SepHelper.memoTypeString(MemoType.MEMO_NONE).equals("none"))
    assert(SepHelper.memoTypeString(MemoType.MEMO_RETURN).equals("return"))
  }

  @ParameterizedTest
  @CsvSource(
    value =
      [
        "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG,",
        "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG:1234,1234",
        "MCEO75Y6YKE53HM6N46IJYH3LK3YYFZ4QWGNUKCSSIQSH3KOAD7BEAAAAAAAAAAAPNT2W,"
      ]
  )
  fun `test valid stellar account`(strAccount: String, expectedMemo: String?) {
    val gotMemo = SepHelper.getAccountMemo(strAccount)
    assertEquals(gotMemo, expectedMemo)
  }

  @ParameterizedTest
  @ValueSource(
    strings =
      [
        "ABC",
        "ABC:123",
        "GCHU3RZAECOKGM2YAJLQIIYB2ZPLMFTTGN5D3XZNX4RDOEERVLXO7H__",
        "MCEO75Y6YKE53HM6N46IJYH3LK3YYFZ4QWGNUKCSSIQSH3KOAD7BEAAAAAAAAAAAPNT2W___",
        "AMCEO75Y6YKE53HM6N46IJYH3LK3YYFZ4QWGNUKCSSIQSH3KOAD7BEAAAAAAAAAAAPNT2W"
      ]
  )
  fun `test invalid stellar account`(strAccount: String) {
    assertThrows<Exception> { SepHelper.getAccountMemo(strAccount) }
  }
}
