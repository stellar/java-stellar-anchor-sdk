package org.stellar.anchor.platform.custody.fireblocks

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import java.time.Instant
import kotlin.test.assertEquals
import org.apache.commons.lang3.StringUtils
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasEntry
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
        .toAccount(TO_ACCOUNT_ID)
        .asset(ASSET_ID)
        .amount(AMOUNT)
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

    JSONAssert.assertEquals(responseJson, gson.toJson(response), JSONCompareMode.STRICT)
  }

  @Test
  fun test_getTransactionsByTimeRange_success() {
    val responseJson =
      getResourceFileAsString("custody/api/payment/fireblocks/two_transactions_response.json")
    val urlCapture = slot<String>()
    val queryParamsCapture = slot<Map<String, String>>()

    every { fireblocksClient.get(capture(urlCapture), capture(queryParamsCapture)) } returns
      responseJson

    val startTime = Instant.now().minusSeconds(5)
    val endTime = Instant.now()
    val response = fireblocksPaymentService.getTransactionsByTimeRange(startTime, endTime)

    Assertions.assertEquals("/v1/transactions", urlCapture.captured)
    Assertions.assertEquals(5, queryParamsCapture.captured.size)

    assertThat(queryParamsCapture.captured, hasEntry("after", startTime.toEpochMilli().toString()))
    assertThat(queryParamsCapture.captured, hasEntry("before", endTime.toEpochMilli().toString()))
    assertThat(queryParamsCapture.captured, hasEntry("limit", "500"))
    assertThat(queryParamsCapture.captured, hasEntry("orderBy", "createdAt"))
    assertThat(queryParamsCapture.captured, hasEntry("sort", "ASC"))

    JSONAssert.assertEquals(responseJson, gson.toJson(response), JSONCompareMode.STRICT)
  }

  @Test
  fun `getTransactionsByTimeRange select number of items equal to limit`() {
    val responseJson =
      getResourceFileAsString("custody/api/payment/fireblocks/two_transactions_response.json")

    FireblocksPaymentService.TRANSACTIONS_LIMIT = 2
    val startTime = Instant.now().minusSeconds(5)
    val endTime = Instant.now()

    val queryParams =
      mapOf(
        "after" to startTime.toEpochMilli().toString(),
        "before" to endTime.toEpochMilli().toString(),
        "limit" to FireblocksPaymentService.TRANSACTIONS_LIMIT.toString(),
        "orderBy" to "createdAt",
        "sort" to "ASC"
      )
    every { fireblocksClient.get(any(), queryParams) } returns responseJson

    val maxCreatedTime = 1684856015569L
    val secondQueryParams =
      mapOf(
        "after" to maxCreatedTime.toString(),
        "before" to endTime.toEpochMilli().toString(),
        "limit" to FireblocksPaymentService.TRANSACTIONS_LIMIT.toString(),
        "orderBy" to "createdAt",
        "sort" to "ASC"
      )
    every { fireblocksClient.get(any(), secondQueryParams) } returns StringUtils.EMPTY

    val response = fireblocksPaymentService.getTransactionsByTimeRange(startTime, endTime)

    JSONAssert.assertEquals(responseJson, gson.toJson(response), JSONCompareMode.STRICT)
    FireblocksPaymentService.TRANSACTIONS_LIMIT = 500
  }

  @Test
  fun `getTransactionsByTimeRange select more than limit`() {
    val twoTransactionsResponseJson =
      getResourceFileAsString("custody/api/payment/fireblocks/two_transactions_response.json")
    val oneTransactionResponseJson =
      getResourceFileAsString("custody/api/payment/fireblocks/one_transaction_response.json")

    FireblocksPaymentService.TRANSACTIONS_LIMIT = 2
    val startTime = Instant.now().minusSeconds(5)
    val endTime = Instant.now()

    val queryParams =
      mapOf(
        "after" to startTime.toEpochMilli().toString(),
        "before" to endTime.toEpochMilli().toString(),
        "limit" to FireblocksPaymentService.TRANSACTIONS_LIMIT.toString(),
        "orderBy" to "createdAt",
        "sort" to "ASC"
      )
    every { fireblocksClient.get(any(), queryParams) } returns twoTransactionsResponseJson

    val maxCreatedTime = 1684856015569L
    val secondQueryParams =
      mapOf(
        "after" to maxCreatedTime.toString(),
        "before" to endTime.toEpochMilli().toString(),
        "limit" to FireblocksPaymentService.TRANSACTIONS_LIMIT.toString(),
        "orderBy" to "createdAt",
        "sort" to "ASC"
      )
    every { fireblocksClient.get(any(), secondQueryParams) } returns oneTransactionResponseJson

    val response = fireblocksPaymentService.getTransactionsByTimeRange(startTime, endTime)
    assertEquals(3, response.size)

    FireblocksPaymentService.TRANSACTIONS_LIMIT = 500
  }

  @Test
  fun `getTransactionsByTimeRange time range validation`() {
    val startTime = Instant.now().minusSeconds(5)
    val endTime = Instant.now()

    val ex =
      assertThrows<IllegalArgumentException> {
        fireblocksPaymentService.getTransactionsByTimeRange(endTime, startTime)
      }
    assertEquals("End time can't be before start time", ex.message)
  }
}
