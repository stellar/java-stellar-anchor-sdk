package org.stellar.anchor.platform

import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.api.exception.ServerErrorException
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.ResourceJsonAssetService
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.platform.observer.PaymentListener
import org.stellar.anchor.platform.observer.stellar.PaymentObservingAccountStore
import org.stellar.anchor.platform.observer.stellar.PaymentObservingAccountsManager
import org.stellar.anchor.platform.observer.stellar.StellarPaymentStreamerCursorStore

class PaymentBeansTest {
  @MockK private lateinit var paymentStreamerCursorStore: StellarPaymentStreamerCursorStore
  @MockK private lateinit var paymentObservingAccountStore: PaymentObservingAccountStore

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxed = true)
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun test_stellarPaymentObserverService_failure() {
    val assetService: AssetService = ResourceJsonAssetService("test_assets.json")
    val paymentBeans = PaymentBeans()
    val mockPaymentListener = mockk<PaymentListener>()
    val mockPaymentListeners = listOf(mockPaymentListener)

    // assetService is null
    var ex =
      assertThrows<ServerErrorException> {
        paymentBeans.stellarPaymentObserver(null, null, null, null, null)
      }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("Asset service cannot be empty.", ex.message)

    // assetService.listAllAssets() is null
    val mockEmptyAssetService = mockk<AssetService>()
    every { mockEmptyAssetService.listAllAssets() } returns null
    ex =
      assertThrows {
        paymentBeans.stellarPaymentObserver(mockEmptyAssetService, null, null, null, null)
      }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("Asset service cannot be empty.", ex.message)

    // assetService.listAllAssets() doesn't contain stellar assets
    val mockStellarLessAssetService = mockk<AssetService>()
    every { mockStellarLessAssetService.listAllAssets() } returns listOf()
    ex =
      assertThrows {
        paymentBeans.stellarPaymentObserver(
          mockStellarLessAssetService,
          null,
          null,
          null,
          null
        )
      }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("Asset service should contain at least one Stellar asset.", ex.message)

    // paymentListeners is null
    ex =
      assertThrows {
        paymentBeans.stellarPaymentObserver(assetService, null, null, null, null)
      }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("The stellar payment observer service needs at least one listener.", ex.message)

    // paymentListeners is empty
    ex =
      assertThrows {
        paymentBeans.stellarPaymentObserver(assetService, listOf(), null, null, null)
      }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("The stellar payment observer service needs at least one listener.", ex.message)

    // paymentStreamerCursorStore is null
    ex =
      assertThrows {
        paymentBeans.stellarPaymentObserver(
          assetService,
          mockPaymentListeners,
          null,
          null,
          null
        )
      }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("Payment streamer cursor store cannot be empty.", ex.message)

    // appConfig is null
    every { paymentStreamerCursorStore.load() } returns null
    ex =
      assertThrows {
        paymentBeans.stellarPaymentObserver(
          assetService,
          mockPaymentListeners,
          paymentStreamerCursorStore,
          null,
          null
        )
      }
    assertInstanceOf(ServerErrorException::class.java, ex)
    assertEquals("App config cannot be empty.", ex.message)
  }

  @Test
  fun test_givenGoodManager_whenConstruct_thenOk() {
    // success!
    val paymentBeans = PaymentBeans()
    val assetService: AssetService = ResourceJsonAssetService("test_assets.json")
    val mockPaymentListener = mockk<PaymentListener>()
    val mockPaymentListeners = listOf(mockPaymentListener)

    val paymentObservingAccountsManager =
      PaymentObservingAccountsManager(
        paymentObservingAccountStore
      )

    val mockAppConfig = mockk<AppConfig>()
    every { mockAppConfig.horizonUrl } returns "https://horizon-testnet.stellar.org"
    assertDoesNotThrow {
      paymentBeans.stellarPaymentObserver(
        assetService,
        mockPaymentListeners,
        paymentStreamerCursorStore,
        paymentObservingAccountsManager,
        mockAppConfig
      )
    }
  }
}
