package org.stellar.anchor.client.service

import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.custody.GenerateDepositAddressResponse
import org.stellar.anchor.api.shared.SepDepositInfo
import org.stellar.anchor.platform.apiclient.CustodyApiClient
import org.stellar.anchor.platform.data.JdbcSep24Transaction

class Sep24DepositInfoCustodyGeneratorTest {

  companion object {
    private const val ADDRESS = "testAccount"
    private const val MEMO = "MTIzZTQ1NjctZTg5Yi0xMmQzLWE0NTYtNDI2NjE0MTc="
    private const val MEMO_TYPE = "hash"
    private const val ASSET_ID = "USDC"
  }

  @Test
  fun test_sep24_custodyGenerator_success() {
    val txn = _root_ide_package_.org.stellar.anchor.platform.data.JdbcSep24Transaction()
    txn.amountInAsset = ASSET_ID
    val custodyApiClient:
      _root_ide_package_.org.stellar.anchor.platform.apiclient.CustodyApiClient =
      mockk()
    val generator =
      _root_ide_package_.org.stellar.anchor.platform.service.Sep24DepositInfoCustodyGenerator(
        custodyApiClient
      )

    val depositAddress = GenerateDepositAddressResponse(ADDRESS, MEMO, MEMO_TYPE)

    every { custodyApiClient.generateDepositAddress(ASSET_ID) } returns depositAddress

    val actualInfo = generator.generate(txn)

    val expectedInfo = SepDepositInfo(ADDRESS, MEMO, MEMO_TYPE)

    assertEquals(expectedInfo, actualInfo)
  }
}
