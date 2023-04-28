package org.stellar.anchor.platform.custody.fireblocks

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.platform.config.FireblocksConfig
import org.stellar.anchor.util.FileUtil.getResourceFileAsString
import org.stellar.anchor.util.GsonUtils

class CustodyApiClientTest {

  companion object {
    private const val VAULT_ACCOUNT_ID = "testVaultAccountId"
    private const val ASSET_ID = "TEST_ASSET_ID"
  }

  private val gson = GsonUtils.getInstance()

  @MockK(relaxed = true) private lateinit var fireblocksClient: FireblocksClient
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
      getResourceFileAsString("custody/fireblocks/create_new_deposit_address_response.json")
    val expectedResponseJson =
      getResourceFileAsString("custody/fireblocks/generated_deposit_address_response.json")
    val requestCapture = slot<String>()
    val urlCapture = slot<String>()
    val requestJson =
      getResourceFileAsString("custody/fireblocks/create_new_deposit_address_request.json")

    every { fireblocksClient.post(capture(urlCapture), capture(requestCapture)) } returns
      responseJson

    val response = fireblocksPaymentService.generateDepositAddress(ASSET_ID)

    Assertions.assertEquals(
      "/vault/accounts/testVaultAccountId/TEST_ASSET_ID/addresses",
      urlCapture.captured
    )
    JSONAssert.assertEquals(requestJson, requestCapture.captured, JSONCompareMode.STRICT)
    JSONAssert.assertEquals(expectedResponseJson, gson.toJson(response), JSONCompareMode.STRICT)
  }
}
