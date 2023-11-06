package org.stellar.anchor.client.service

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.sep.AssetInfo
import org.stellar.anchor.api.shared.SepDepositInfo
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.platform.data.JdbcSep6Transaction

class Sep6DepositInfoSelfGeneratorTest {

  companion object {
    private val TXN_ID = "testId"
    private const val ASSET_CODE = "USDC"
    private const val ASSET_ISSUER = "testIssuer"
    private const val DISTRIBUTION_ACCOUNT = "testAccount"
  }

  @MockK(relaxed = true) lateinit var assetService: AssetService

  private lateinit var generator:
    _root_ide_package_.org.stellar.anchor.platform.service.Sep6DepositInfoSelfGenerator

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    val asset = mockk<AssetInfo>()
    every { asset.distributionAccount } returns DISTRIBUTION_ACCOUNT
    every { assetService.getAsset(ASSET_CODE, ASSET_ISSUER) } returns asset
    generator =
      _root_ide_package_.org.stellar.anchor.platform.service.Sep6DepositInfoSelfGenerator(
        assetService
      )
  }

  @Test
  fun test_sep6_custodyGenerator_success() {
    val txn = _root_ide_package_.org.stellar.anchor.platform.data.JdbcSep6Transaction()
    txn.id = TXN_ID
    txn.requestAssetCode = ASSET_CODE
    txn.requestAssetIssuer = ASSET_ISSUER

    val result = generator.generate(txn)
    val expected =
      SepDepositInfo(DISTRIBUTION_ACCOUNT, "MDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDB0ZXN0SWQ=", "hash")
    assertEquals(expected, result)
  }
}
