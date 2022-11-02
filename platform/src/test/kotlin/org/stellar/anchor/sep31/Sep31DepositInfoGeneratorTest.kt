package org.stellar.anchor.sep31

import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.stellar.anchor.api.callback.CustomerIntegration
import org.stellar.anchor.api.callback.FeeIntegration
import org.stellar.anchor.api.sep.sep31.Sep31DepositInfo
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.ResourceJsonAssetService
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.config.Sep31Config
import org.stellar.anchor.event.EventService
import org.stellar.anchor.platform.data.JdbcSep31Transaction
import org.stellar.anchor.platform.service.Sep31DepositInfoGeneratorSelf
import org.stellar.anchor.sep38.Sep38QuoteStore
import org.stellar.anchor.util.GsonUtils
import java.util.*

class Sep31DepositInfoGeneratorTest {
  companion object {
    val gson: Gson = GsonUtils.getInstance()

    private const val txnJson =
        """
        {
          "id": "a2392add-87c9-42f0-a5c1-5f1728030b68",
          "status": "pending_sender",
          "stellar_account_id": "GAYR3FVW2PCXTNHHWHEAFOCKZQV4PEY2ZKGIKB47EKPJ3GSBYA52XJBY",
          "client_domain": "demo-wallet-server.stellar.org",
          "fields": {
            "receiver_account_number": "1",
            "type": "SWIFT",
            "receiver_routing_number": "1"
          },
          "stellarTransactions": []
        }
    """
  }

  private val assetService: AssetService = ResourceJsonAssetService("test_assets.json")

  @MockK(relaxed = true) private lateinit var txnStore: Sep31TransactionStore
  @MockK(relaxed = true) private lateinit var appConfig: AppConfig
  @MockK(relaxed = true) private lateinit var sep31Config: Sep31Config
  @MockK(relaxed = true) private lateinit var sep31DepositInfoGenerator: Sep31DepositInfoGenerator
  @MockK(relaxed = true) private lateinit var quoteStore: Sep38QuoteStore
  @MockK(relaxed = true) private lateinit var feeIntegration: FeeIntegration
  @MockK(relaxed = true) private lateinit var customerIntegration: CustomerIntegration
  @MockK(relaxed = true) private lateinit var eventPublishService: EventService
  @MockK(relaxed = true) private lateinit var txn: Sep31Transaction

  private lateinit var sep31Service: Sep31Service

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    sep31Service =
        Sep31Service(
            appConfig,
            sep31Config,
            txnStore,
            sep31DepositInfoGenerator,
            quoteStore,
            assetService,
            feeIntegration,
            customerIntegration,
            eventPublishService)

    txn = gson.fromJson(txnJson, JdbcSep31Transaction::class.java)
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun test_updateDepositInfo_self() {
    sep31Service =
        Sep31Service(
            appConfig,
            sep31Config,
            txnStore,
            Sep31DepositInfoGeneratorSelf(), // set deposit info generator
            quoteStore,
            assetService,
            feeIntegration,
            customerIntegration,
            eventPublishService)

    Assertions.assertEquals("a2392add-87c9-42f0-a5c1-5f1728030b68", txn.id)
    Assertions.assertEquals(
        "GAYR3FVW2PCXTNHHWHEAFOCKZQV4PEY2ZKGIKB47EKPJ3GSBYA52XJBY", txn.stellarAccountId)
    Assertions.assertNull(txn.stellarMemoType)
    Assertions.assertNull(txn.stellarMemo)

    var wantMemo = StringUtils.truncate("a2392add-87c9-42f0-a5c1-5f1728030b68", 32)
    wantMemo = String(Base64.getEncoder().encode(wantMemo.toByteArray()))
    Assertions.assertEquals("YTIzOTJhZGQtODdjOS00MmYwLWE1YzEtNWYxNzI4MDM=", wantMemo)

    Sep31Service.Context.get().transaction = txn
    assertDoesNotThrow { sep31Service.updateDepositInfo() }

    Assertions.assertEquals(
        "GAYR3FVW2PCXTNHHWHEAFOCKZQV4PEY2ZKGIKB47EKPJ3GSBYA52XJBY", txn.stellarAccountId)
    Assertions.assertEquals("hash", txn.stellarMemoType)
    Assertions.assertEquals("YTIzOTJhZGQtODdjOS00MmYwLWE1YzEtNWYxNzI4MDM=", txn.stellarMemo)
  }

  @ParameterizedTest
  @CsvSource(
      value =
          [",none", "YTIzOTJhZGQtODdjOS00MmYwLWE1YzEtNWYxNzI4MDM=,hash", "123,id", "John Doe,text"])
  fun test_updateDepositInfo_api(memo: String?, memoType: String?) {
    val nonEmptyMemo = Objects.toString(memo, "")
    val nonEmptyMemoType = Objects.toString(memoType, "")
    every { sep31DepositInfoGenerator.generate(any()) } returns
        Sep31DepositInfo(
            "GAYR3FVW2PCXTNHHWHEAFOCKZQV4PEY2ZKGIKB47EKPJ3GSBYA52XJBY",
            nonEmptyMemo,
            nonEmptyMemoType)

    Assertions.assertEquals("a2392add-87c9-42f0-a5c1-5f1728030b68", txn.id)
    Assertions.assertEquals(
        "GAYR3FVW2PCXTNHHWHEAFOCKZQV4PEY2ZKGIKB47EKPJ3GSBYA52XJBY", txn.stellarAccountId)
    Assertions.assertNull(txn.stellarMemoType)
    Assertions.assertNull(txn.stellarMemo)

    var wantMemo = StringUtils.truncate("a2392add-87c9-42f0-a5c1-5f1728030b68", 32)
    wantMemo = String(Base64.getEncoder().encode(wantMemo.toByteArray()))
    Assertions.assertEquals("YTIzOTJhZGQtODdjOS00MmYwLWE1YzEtNWYxNzI4MDM=", wantMemo)

    Sep31Service.Context.get().transaction = txn
    assertDoesNotThrow { sep31Service.updateDepositInfo() }

    Assertions.assertEquals(
        "GAYR3FVW2PCXTNHHWHEAFOCKZQV4PEY2ZKGIKB47EKPJ3GSBYA52XJBY", txn.stellarAccountId)
    Assertions.assertEquals(nonEmptyMemo, txn.stellarMemo)
    Assertions.assertEquals(nonEmptyMemoType, txn.stellarMemoType)
  }
}
