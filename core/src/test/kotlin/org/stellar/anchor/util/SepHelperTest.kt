package org.stellar.anchor.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
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
  @ValueSource(
    strings =
      [
        "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
        "MCEO75Y6YKE53HM6N46IJYH3LK3YYFZ4QWGNUKCSSIQSH3KOAD7BEAAAAAAAAAAAPNT2W"
      ]
  )
  fun `test valid stellar account`(strAccount: String) {
    SepHelper.getAccountMemo(strAccount)
  }
}
