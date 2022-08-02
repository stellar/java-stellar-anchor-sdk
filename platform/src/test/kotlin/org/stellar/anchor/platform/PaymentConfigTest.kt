package org.stellar.anchor.platform

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.ServerErrorException
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.ResourceJsonAssetService
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.platform.payment.observer.PaymentListener
import org.stellar.anchor.platform.payment.observer.stellar.StellarPaymentStreamerCursorStore

class PaymentConfigTest {
  @Test
  fun test_stellarPaymentObserverService_failure() {
    val paymentConfig = PaymentConfig()

    // assetService is null
    var ex =
      assertThrows<ServerErrorException> {
        paymentConfig.stellarPaymentObserverService(null, null, null, null)
      }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("Asset service cannot be empty.", ex.message)

    // assetService.listAllAssets() is null
    val mockEmptyAssetService = mockk<AssetService>()
    every { mockEmptyAssetService.listAllAssets() } returns null
    ex =
      assertThrows {
        paymentConfig.stellarPaymentObserverService(mockEmptyAssetService, null, null, null)
      }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("Asset service cannot be empty.", ex.message)

    // assetService.listAllAssets() doesn't contain stellar assets
    val mockStellarLessAssetService = mockk<AssetService>()
    every { mockStellarLessAssetService.listAllAssets() } returns listOf()
    ex =
      assertThrows {
        paymentConfig.stellarPaymentObserverService(mockStellarLessAssetService, null, null, null)
      }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("Asset service should contain at least one Stellar asset.", ex.message)

    // paymentListeners is null
    val assetService: AssetService = ResourceJsonAssetService("test_assets.json")
    ex =
      assertThrows { paymentConfig.stellarPaymentObserverService(assetService, null, null, null) }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("The stellar payment observer service needs at least one listener.", ex.message)

    // paymentListeners is empty
    ex =
      assertThrows {
        paymentConfig.stellarPaymentObserverService(assetService, listOf(), null, null)
      }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("The stellar payment observer service needs at least one listener.", ex.message)

    // paymentStreamerCursorStore is null
    val mockPaymentListener = mockk<PaymentListener>()
    val mockPaymentListeners = listOf(mockPaymentListener)
    ex =
      assertThrows {
        paymentConfig.stellarPaymentObserverService(assetService, mockPaymentListeners, null, null)
      }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("Payment streamer cursor store cannot be empty.", ex.message)

    // appConfig is null
    val mockPaymentStreamerCursorStore = mockk<StellarPaymentStreamerCursorStore>()
    every { mockPaymentStreamerCursorStore.load() } returns null
    ex =
      assertThrows {
        paymentConfig.stellarPaymentObserverService(
          assetService,
          mockPaymentListeners,
          mockPaymentStreamerCursorStore,
          null
        )
      }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("App config cannot be empty.", ex.message)

    // success!
    val mockAppConfig = mockk<AppConfig>()
    every { mockAppConfig.horizonUrl } returns "https://horizon-testnet.stellar.org"
    assertDoesNotThrow {
      paymentConfig.stellarPaymentObserverService(
        assetService,
        mockPaymentListeners,
        mockPaymentStreamerCursorStore,
        mockAppConfig
      )
    }
  }
}
