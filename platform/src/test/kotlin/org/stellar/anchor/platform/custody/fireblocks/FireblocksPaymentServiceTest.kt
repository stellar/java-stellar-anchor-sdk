package org.stellar.anchor.platform.custody.fireblocks

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.exception.FireblocksException
import org.stellar.anchor.platform.config.FireblocksConfig
import org.stellar.anchor.platform.data.JdbcCustodyTransaction
import org.stellar.anchor.util.FileUtil.getResourceFileAsString
import org.stellar.anchor.util.GsonUtils

class FireblocksPaymentServiceTest {

  companion object {
    private const val VAULT_ACCOUNT_ID = "testVaultAccountId"
    private const val ASSET_ID = "TEST_ASSET_ID"
    private const val TO_ACCOUNT_ID = "toVaultAccountId"
    private const val AMOUNT = "10"
    private const val EXTERNAL_TXN_ID = "TRANSACTION_ID"
  }

  private val gson = GsonUtils.getInstance()

  @MockK(relaxed = true) private lateinit var fireblocksClient: FireblocksApiClient
  @MockK(relaxed = true) private lateinit var fireblocksConfig: FireblocksConfig

  private lateinit var fireblocksPaymentService: FireblocksPaymentService

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    every { fireblocksConfig.vaultAccountId } returns VAULT_ACCOUNT_ID

    fireblocksPaymentService = FireblocksPaymentService(fireblocksClient, fireblocksConfig)
  }

  @Test
  fun test_generateDepositAddress_success() {
    val responseJson =
      getResourceFileAsString(
        "custody/api/address/fireblocks/create_new_deposit_address_response.json"
      )
    val expectedResponseJson =
      getResourceFileAsString(
        "custody/api/address/fireblocks/generated_deposit_address_response.json"
      )
    val requestCapture = slot<String>()
    val urlCapture = slot<String>()
    val requestJson =
      getResourceFileAsString(
        "custody/api/address/fireblocks/create_new_deposit_address_request.json"
      )

    every { fireblocksClient.post(capture(urlCapture), capture(requestCapture)) } returns
      responseJson
    every { fireblocksConfig.getFireblocksAssetCode(ASSET_ID) } returns ASSET_ID

    val response = fireblocksPaymentService.generateDepositAddress(ASSET_ID)

    Assertions.assertEquals(
      "/v1/vault/accounts/testVaultAccountId/TEST_ASSET_ID/addresses",
      urlCapture.captured
    )
    JSONAssert.assertEquals(requestJson, requestCapture.captured, JSONCompareMode.STRICT)
    JSONAssert.assertEquals(expectedResponseJson, gson.toJson(response), JSONCompareMode.STRICT)
  }

  @Test
  fun test_createTransactionPayment_success() {
    val requestJson =
      getResourceFileAsString(
        "custody/api/payment/fireblocks/create_new_transaction_payment_request.json"
      )
    val responseJson =
      getResourceFileAsString(
        "custody/api/payment/fireblocks/created_new_transaction_payment_response.json"
      )
    val expectedResponse = "{\"id\":\"1\"}"
    val requestCapture = slot<String>()
    val urlCapture = slot<String>()

    every { fireblocksClient.post(capture(urlCapture), capture(requestCapture)) } returns
      responseJson
    every { fireblocksConfig.getFireblocksAssetCode(ASSET_ID) } returns ASSET_ID

    val transaction =
      JdbcCustodyTransaction.builder()
        .fromAccount(VAULT_ACCOUNT_ID)
        .amountOutAsset(ASSET_ID)
        .toAccount(TO_ACCOUNT_ID)
        .amountOut(AMOUNT)
        .build()
    val response = fireblocksPaymentService.createTransactionPayment(transaction, StringUtils.EMPTY)

    Assertions.assertEquals("/v1/transactions", urlCapture.captured)

    JSONAssert.assertEquals(requestJson, requestCapture.captured, JSONCompareMode.STRICT)
    JSONAssert.assertEquals(expectedResponse, gson.toJson(response), JSONCompareMode.STRICT)
  }

  @Test
  fun test_createTransactionPayment_failure() {
    val responseJson = getResourceFileAsString("custody/api/payment/fireblocks/error_response.json")
    val expectedResponse = "{\"id\":\"\"}"

    every { fireblocksClient.post(any(), any()) } returns responseJson

    val transaction = JdbcCustodyTransaction()
    val response = fireblocksPaymentService.createTransactionPayment(transaction, StringUtils.EMPTY)

    JSONAssert.assertEquals(expectedResponse, gson.toJson(response), JSONCompareMode.STRICT)
  }

  @Test
  fun test_createTransactionPayment_IOException() {
    val expectedMessage =
      "Fireblocks API returned an error. HTTP status[503], response[Fireblocks service is unavailable]"

    every { fireblocksClient.post(any(), any()) } throws
      FireblocksException("Fireblocks service is unavailable", 503)

    val transaction = JdbcCustodyTransaction()
    val exception =
      assertThrows<FireblocksException> {
        fireblocksPaymentService.createTransactionPayment(transaction, StringUtils.EMPTY)
      }

    assertEquals(expectedMessage, exception.message)
  }

  @Test
  fun test_getTransactionById_success() {
    val responseJson =
      getResourceFileAsString("custody/api/payment/fireblocks/get_transaction_response.json")
    val urlCapture = slot<String>()

    every { fireblocksClient.get(capture(urlCapture)) } returns responseJson

    val response = fireblocksPaymentService.getTransactionById(EXTERNAL_TXN_ID)

    Assertions.assertEquals(
      String.format("/v1/transactions/%s", EXTERNAL_TXN_ID),
      urlCapture.captured
    )

    assertNotNull(response)
  }
}
