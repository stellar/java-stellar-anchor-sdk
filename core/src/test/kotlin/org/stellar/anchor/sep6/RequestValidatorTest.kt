package org.stellar.anchor.sep6

import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.stellar.anchor.TestConstants.Companion.TEST_ACCOUNT
import org.stellar.anchor.TestConstants.Companion.TEST_ASSET
import org.stellar.anchor.TestConstants.Companion.TEST_MEMO
import org.stellar.anchor.api.callback.CustomerIntegration
import org.stellar.anchor.api.callback.GetCustomerRequest
import org.stellar.anchor.api.callback.GetCustomerResponse
import org.stellar.anchor.api.exception.SepCustomerInfoNeededException
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.api.exception.SepValidationException
import org.stellar.anchor.api.exception.ServerErrorException
import org.stellar.anchor.api.sep.AssetInfo
import org.stellar.anchor.api.sep.sep12.Sep12Status
import org.stellar.anchor.api.shared.CustomerField
import org.stellar.anchor.asset.AssetService

class RequestValidatorTest {
  @MockK(relaxed = true) lateinit var assetService: AssetService
  @MockK(relaxed = true) lateinit var customerIntegration: CustomerIntegration

  private lateinit var requestValidator: RequestValidator

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    requestValidator = RequestValidator(assetService, customerIntegration)
  }

  @Test
  fun `test getDepositAsset`() {
    val asset = mockk<AssetInfo>()
    val deposit = mockk<AssetInfo.DepositOperation>()
    every { asset.sep6Enabled } returns true
    every { asset.deposit } returns deposit
    every { deposit.enabled } returns true
    every { assetService.getAsset(TEST_ASSET) } returns asset
    requestValidator.getDepositAsset(TEST_ASSET)
  }

  @Test
  fun `test getDepositAsset with invalid asset code`() {
    every { assetService.getAsset(TEST_ASSET) } returns null
    assertThrows<SepValidationException> { requestValidator.getDepositAsset(TEST_ASSET) }
  }

  @Test
  fun `test getDepositAsset with deposit disabled asset`() {
    val asset = mockk<AssetInfo>()
    val deposit = mockk<AssetInfo.DepositOperation>()
    every { asset.sep6Enabled } returns true
    every { asset.deposit } returns deposit
    every { deposit.enabled } returns false
    every { assetService.getAsset(TEST_ASSET) } returns asset
    assertThrows<SepValidationException> { requestValidator.getDepositAsset(TEST_ASSET) }
  }

  @Test
  fun `test getDepositAsset with sep6 disabled asset`() {
    val asset = mockk<AssetInfo>()
    every { asset.sep6Enabled } returns false
    every { assetService.getAsset(TEST_ASSET) } returns asset
    assertThrows<SepValidationException> { requestValidator.getDepositAsset(TEST_ASSET) }
  }

  @Test
  fun `test getWithdrawAsset`() {
    val asset = mockk<AssetInfo>()
    val withdraw = mockk<AssetInfo.WithdrawOperation>()
    every { asset.sep6Enabled } returns true
    every { asset.withdraw } returns withdraw
    every { withdraw.enabled } returns true
    every { assetService.getAsset(TEST_ASSET) } returns asset
    requestValidator.getWithdrawAsset(TEST_ASSET)
  }

  @Test
  fun `test getWithdrawAsset with invalid asset code`() {
    every { assetService.getAsset(TEST_ASSET) } returns null
    assertThrows<SepValidationException> { requestValidator.getWithdrawAsset(TEST_ASSET) }
  }

  @Test
  fun `test getWithdrawAsset with withdraw disabled asset`() {
    val asset = mockk<AssetInfo>()
    val withdraw = mockk<AssetInfo.WithdrawOperation>()
    every { asset.sep6Enabled } returns true
    every { asset.withdraw } returns withdraw
    every { withdraw.enabled } returns false
    every { assetService.getAsset(TEST_ASSET) } returns asset
    assertThrows<SepValidationException> { requestValidator.getWithdrawAsset(TEST_ASSET) }
  }

  @Test
  fun `test getWithdrawAsset with sep6 disabled asset`() {
    val asset = mockk<AssetInfo>()
    every { asset.sep6Enabled } returns false
    every { assetService.getAsset(TEST_ASSET) } returns asset
    assertThrows<SepValidationException> { requestValidator.getWithdrawAsset(TEST_ASSET) }
  }

  @ParameterizedTest
  @ValueSource(strings = ["1", "100", "1.00", "100.00", "50"])
  fun `test validateAmount`(amount: String) {
    requestValidator.validateAmount(amount, TEST_ASSET, 2, 1L, 100L)
  }

  @Test
  fun `test validateAmount with too high precision`() {
    assertThrows<SepValidationException> {
      requestValidator.validateAmount("1.000001", TEST_ASSET, 2, 1L, 100L)
    }
  }

  @Test
  fun `test validateAmount with too high value`() {
    assertThrows<SepValidationException> {
      requestValidator.validateAmount("101", TEST_ASSET, 2, 1L, 100L)
    }
  }

  @Test
  fun `test validateAmount with too low value`() {
    assertThrows<SepValidationException> {
      requestValidator.validateAmount("0", TEST_ASSET, 2, 1L, 100L)
    }
  }

  @ValueSource(strings = ["bank_account", "cash"])
  @ParameterizedTest
  fun `test validateTypes`(type: String) {
    requestValidator.validateTypes(type, TEST_ASSET, listOf("bank_account", "cash"))
  }

  @Test
  fun `test validateTypes with invalid type`() {
    assertThrows<SepValidationException> {
      requestValidator.validateTypes("??", TEST_ASSET, listOf("bank_account", "cash"))
    }
  }

  @Test
  fun `test validateAccount`() {
    every {
      customerIntegration.getCustomer(GetCustomerRequest.builder().account(TEST_ACCOUNT).build())
    } returns GetCustomerResponse.builder().status(Sep12Status.ACCEPTED.name).build()
    requestValidator.validateAccount(TEST_ACCOUNT)
  }

  @Test
  fun `test validateAccount with invalid account`() {
    assertThrows<SepValidationException> { requestValidator.validateAccount("??") }

    verify { customerIntegration wasNot called }
  }

  @Test
  fun `test validateKyc`() {
    every {
      customerIntegration.getCustomer(
        GetCustomerRequest.builder().account(TEST_ACCOUNT).memo(TEST_MEMO).memoType("id").build()
      )
    } returns GetCustomerResponse.builder().id("123").status(Sep12Status.ACCEPTED.name).build()
    assertEquals("123", requestValidator.validateKyc(TEST_ACCOUNT, TEST_MEMO))
  }

  @Test
  fun `test validateKyc customerIntegration failure`() {
    every {
      customerIntegration.getCustomer(
        GetCustomerRequest.builder().account(TEST_ACCOUNT).memo(TEST_MEMO).memoType("id").build()
      )
    } throws RuntimeException("test")
    assertThrows<RuntimeException> { requestValidator.validateKyc(TEST_ACCOUNT, TEST_MEMO) }
  }

  @Test
  fun `test validateKyc with needs info customer`() {
    every {
      customerIntegration.getCustomer(
        GetCustomerRequest.builder().account(TEST_ACCOUNT).memo(TEST_MEMO).memoType("id").build()
      )
    } returns
      GetCustomerResponse.builder()
        .status(Sep12Status.NEEDS_INFO.name)
        .fields(mapOf("first_name" to CustomerField.builder().build()))
        .build()
    val ex =
      assertThrows<SepCustomerInfoNeededException> {
        requestValidator.validateKyc(TEST_ACCOUNT, TEST_MEMO)
      }
    assertEquals(listOf("first_name"), ex.fields)
  }

  @Test
  fun `test validateKyc with processing customer`() {
    every {
      customerIntegration.getCustomer(
        GetCustomerRequest.builder().account(TEST_ACCOUNT).memo(TEST_MEMO).memoType("id").build()
      )
    } returns GetCustomerResponse.builder().status(Sep12Status.PROCESSING.name).build()
    assertThrows<SepNotAuthorizedException> {
      requestValidator.validateKyc(TEST_ACCOUNT, TEST_MEMO)
    }
  }

  @Test
  fun `test validateKyc with rejected customer`() {
    every {
      customerIntegration.getCustomer(
        GetCustomerRequest.builder().account(TEST_ACCOUNT).memo(TEST_MEMO).memoType("id").build()
      )
    } returns GetCustomerResponse.builder().status(Sep12Status.REJECTED.name).build()
    assertThrows<SepNotAuthorizedException> {
      requestValidator.validateKyc(TEST_ACCOUNT, TEST_MEMO)
    }
  }

  @Test
  fun `test validateKyc with unknown status customer`() {
    every {
      customerIntegration.getCustomer(
        GetCustomerRequest.builder().account(TEST_ACCOUNT).memo(TEST_MEMO).memoType("id").build()
      )
    } returns GetCustomerResponse.builder().status("??").build()
    assertThrows<ServerErrorException> { requestValidator.validateKyc(TEST_ACCOUNT, TEST_MEMO) }
  }

  @Test
  fun `test validateKyc without memo`() {
    every {
      customerIntegration.getCustomer(GetCustomerRequest.builder().account(TEST_ACCOUNT).build())
    } returns GetCustomerResponse.builder().id("123").status(Sep12Status.ACCEPTED.name).build()
    assertEquals("123", requestValidator.validateKyc(TEST_ACCOUNT, null))
  }
}
