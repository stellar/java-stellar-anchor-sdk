package org.stellar.anchor.platform.service

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.shared.SepDepositInfo
import org.stellar.anchor.platform.data.JdbcSep31Transaction

class Sep31DepositInfoGeneratoCustodyTest {

  companion object {
    private const val TX_ID = "123e4567-e89b-12d3-a456-426614174000"
    private const val ADDRESS = "testAccount"
    private const val MEMO = "MTIzZTQ1NjctZTg5Yi0xMmQzLWE0NTYtNDI2NjE0MTc="
    private const val MEMO_TYPE = "hash"
  }

  @Test
  fun test_sep31_selfGenerator_success() {
    val txn = JdbcSep31Transaction()
    txn.id = TX_ID
    txn.stellarAccountId = ADDRESS
    val generator = Sep31DepositInfoSelfGenerator()

    val actualInfo = generator.generate(txn)

    val expectedInfo = SepDepositInfo(ADDRESS, MEMO, MEMO_TYPE)

    assertEquals(expectedInfo, actualInfo)
  }
}
