package org.stellar.anchor.util

import org.junit.jupiter.api.Test
import org.stellar.sdk.xdr.MemoType

internal class SepUtilsTest {
    @Test
    fun test() {
        assert(SepUtil.memoTypeString(MemoType.MEMO_ID).equals("id"))
        assert(SepUtil.memoTypeString(MemoType.MEMO_HASH).equals("hash"))
        assert(SepUtil.memoTypeString(MemoType.MEMO_TEXT).equals("text"))
        assert(SepUtil.memoTypeString(MemoType.MEMO_NONE).equals("none"))
        assert(SepUtil.memoTypeString(MemoType.MEMO_RETURN).equals("return"))
    }
}