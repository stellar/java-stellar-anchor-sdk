package org.stellar.anchor.util

import io.mockk.every
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.LockAndMockStatic
import org.stellar.anchor.api.exception.InvalidConfigException
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

  @Test
  @LockAndMockStatic([NetUtil::class])
  fun `readToml throws InvalidConfigException when fetching instance metadata instead of TOML`() {
    val mockUrl = "http://169.254.169.254/latest/meta-data/local-hostname"
    val instanceMetadataContent =
      """
        hostname: ec2-instance-hostname
        ami-id: ami-1234567890abcdef0
        instance-id: i-1234567890abcdef0
        instance-type: t2.micro
        """
        .trimIndent()
    mockkStatic(NetUtil::class)
    every { NetUtil.fetch(mockUrl) } returns instanceMetadataContent

    var exceptionThrown = false
    try {
      Sep1Helper.readToml(mockUrl)
    } catch (e: InvalidConfigException) {
      exceptionThrown = true
    }
    assertTrue(exceptionThrown, "InvalidConfigException was expected but not thrown")
  }
}
