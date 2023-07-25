package org.stellar.anchor

import com.google.gson.Gson
import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.TestConstants.Companion.TEST_ACCOUNT
import org.stellar.anchor.TestConstants.Companion.TEST_ASSET
import org.stellar.anchor.api.exception.NotFoundException
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.api.exception.SepValidationException
import org.stellar.anchor.api.sep.sep6.GetTransactionRequest
import org.stellar.anchor.api.sep.sep6.GetTransactionsRequest
import org.stellar.anchor.api.sep.sep6.InfoResponse
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.config.Sep6Config
import org.stellar.anchor.sep6.PojoSep6RefundPayment
import org.stellar.anchor.sep6.PojoSep6Refunds
import org.stellar.anchor.sep6.PojoSep6Transaction
import org.stellar.anchor.sep6.Sep6Service
import org.stellar.anchor.sep6.Sep6Transaction
import org.stellar.anchor.sep6.Sep6TransactionStore
import org.stellar.anchor.util.GsonUtils

class Sep6ServiceTest {
  companion object {
    val gson: Gson = GsonUtils.getInstance()
  }

  private val assetService: AssetService = DefaultAssetService.fromJsonResource("test_assets.json")

  @MockK(relaxed = true) lateinit var sep6Config: Sep6Config
  @MockK(relaxed = true) lateinit var txnStore: Sep6TransactionStore

  private lateinit var sep6Service: Sep6Service

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { sep6Config.features.isAccountCreation } returns false
    every { sep6Config.features.isClaimableBalances } returns false
    sep6Service = Sep6Service(sep6Config, assetService, txnStore)
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
  }

  private val infoJson =
    """
      {
        "deposit": {
          "USDC": {
            "enabled": true,
            "authentication_required": true,
            "fields": {
              "type": {
                "description": "type of deposit to make",
                "choices": [
                  "SEPA",
                  "SWIFT"
                ],
                "optional": false
              }
            }
          }
        },
        "deposit-exchange": {
          "USDC": {
            "enabled": true,
            "authentication_required": true,
            "fields": {
              "type": {
                "description": "type of deposit to make",
                "choices": [
                  "SEPA",
                  "SWIFT"
                ],
                "optional": false
              }
            }
          }
        },
        "withdraw": {
          "USDC": {
            "enabled": true,
            "authentication_required": true,
            "types": {
              "cash": {},
              "bank_account": {}
            }
          }
        },
        "withdraw-exchange": {
          "USDC": {
            "enabled": true,
            "authentication_required": true,
            "types": {
              "cash": {},
              "bank_account": {}
            }
          }
        },
        "fee": {
          "enabled": false,
          "description": "Fee endpoint is not supported."
        },
        "transactions": {
          "enabled": true,
          "authentication_required": true
        },
        "transaction": {
          "enabled": true,
          "authentication_required": true
        },
        "features": {
          "account_creation": false,
          "claimable_balances": false
        }
      }
    """
      .trimIndent()

  @Test
  fun `test INFO response`() {
    val infoResponse = sep6Service.info
    assertEquals(gson.fromJson(infoJson, InfoResponse::class.java), infoResponse)
  }

  @Test
  fun `test find transaction by id`() {
    val depositTxn = createDepositTxn(TEST_ACCOUNT)
    val request = GetTransactionRequest.builder().id(depositTxn.id).lang("en-US").build()
    every { txnStore.findByTransactionId(depositTxn.id) } returns depositTxn

    sep6Service.findTransaction(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)

    verify { txnStore.findByTransactionId(depositTxn.id) }
  }

  @Test
  fun `test find transaction by stellar transaction id`() {
    val depositTxn = createDepositTxn(TEST_ACCOUNT)
    val request =
      GetTransactionRequest.builder()
        .stellarTransactionId(depositTxn.stellarTransactionId)
        .lang("en-US")
        .build()
    every { txnStore.findByStellarTransactionId(depositTxn.stellarTransactionId) } returns
      depositTxn

    sep6Service.findTransaction(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)

    verify { txnStore.findByStellarTransactionId(depositTxn.stellarTransactionId) }
  }

  @Test
  fun `test find transaction by external transaction id`() {
    val depositTxn = createDepositTxn(TEST_ACCOUNT)
    val request =
      GetTransactionRequest.builder()
        .externalTransactionId(depositTxn.externalTransactionId)
        .lang("en-US")
        .build()
    every { txnStore.findByExternalTransactionId(depositTxn.externalTransactionId) } returns
      depositTxn

    sep6Service.findTransaction(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)

    verify { txnStore.findByExternalTransactionId(depositTxn.externalTransactionId) }
  }

  @Test
  fun `test find transaction missing ids`() {
    val request = GetTransactionRequest.builder().lang("en-US").build()

    assertThrows<SepValidationException> {
      sep6Service.findTransaction(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }
    verify { txnStore wasNot Called }
  }

  @Test
  fun `test find transaction with non-existent id`() {
    val request =
      GetTransactionRequest.builder().id(UUID.randomUUID().toString()).lang("en-US").build()
    every { txnStore.findByTransactionId(any()) } returns null

    assertThrows<NotFoundException> {
      sep6Service.findTransaction(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }

    verify { txnStore.findByTransactionId(any()) }
  }

  @Test
  fun `test find transaction belonging to different account`() {
    val depositTxn = createDepositTxn("other-account")
    val request = GetTransactionRequest.builder().id(depositTxn.id).lang("en-US").build()
    every { txnStore.findByTransactionId(depositTxn.id) } returns depositTxn

    assertThrows<NotFoundException> {
      sep6Service.findTransaction(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }

    verify { txnStore.findByTransactionId(depositTxn.id) }
  }

  @Test
  fun `test find transaction with different account memo`() {
    val depositTxn = createDepositTxn(TEST_ACCOUNT, "other-memo")
    val request = GetTransactionRequest.builder().id(depositTxn.id).lang("en-US").build()
    every { txnStore.findByTransactionId(depositTxn.id) } returns depositTxn

    assertThrows<NotFoundException> {
      sep6Service.findTransaction(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }

    verify { txnStore.findByTransactionId(depositTxn.id) }
  }

  @Test
  fun `test find transactions with account mismatch`() {
    val request =
      GetTransactionsRequest.builder()
        .assetCode(TEST_ASSET)
        .account(TEST_ACCOUNT)
        .noOlderThan(Instant.now().toString())
        .limit(10)
        .kind("deposit")
        .pagingId("1")
        .lang("en-US")
        .build()

    assertThrows<SepNotAuthorizedException> {
      sep6Service.findTransactions(TestHelper.createSep10Jwt("other-account"), request)
    }
    verify { txnStore wasNot Called }
  }

  @Test
  fun `test find transactions with unsupported asset`() {
    val request =
      GetTransactionsRequest.builder()
        .assetCode("???")
        .account(TEST_ACCOUNT)
        .noOlderThan(Instant.now().toString())
        .limit(10)
        .kind("deposit")
        .pagingId("1")
        .lang("en-US")
        .build()

    assertThrows<SepValidationException> {
      sep6Service.findTransactions(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }
    verify { txnStore wasNot Called }
  }

  @Test
  fun `test find transactions`() {
    val depositTxn = createDepositTxn(TEST_ACCOUNT)
    every { txnStore.findTransactions(TEST_ACCOUNT, any(), any()) } returns listOf(depositTxn)
    val request =
      GetTransactionsRequest.builder()
        .assetCode(TEST_ASSET)
        .account(TEST_ACCOUNT)
        .noOlderThan(Instant.now().toString())
        .limit(10)
        .kind("deposit")
        .pagingId("1")
        .lang("en-US")
        .build()
    val response = sep6Service.findTransactions(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)

    verify(exactly = 1) { txnStore.findTransactions(TEST_ACCOUNT, null, request) }

    assert(response.transactions.size == 1)
    assertEquals(depositTxn.id, response.transactions[0].id)
    assertEquals(depositTxn.kind, response.transactions[0].kind)
    assertEquals(depositTxn.status, response.transactions[0].status)
    assertEquals(depositTxn.statusEta, response.transactions[0].statusEta)
    assertEquals(depositTxn.moreInfoUrl, response.transactions[0].moreInfoUrl)
    assertEquals(depositTxn.amountIn, response.transactions[0].amountIn)
    assertEquals(depositTxn.amountInAsset, response.transactions[0].amountInAsset)
    assertEquals(depositTxn.amountOut, response.transactions[0].amountOut)
    assertEquals(depositTxn.amountOutAsset, response.transactions[0].amountOutAsset)
    assertEquals(depositTxn.amountFee, response.transactions[0].amountFee)
    assertEquals(depositTxn.amountFeeAsset, response.transactions[0].amountFeeAsset)
    assertEquals(depositTxn.fromAccount, response.transactions[0].from)
    assertEquals(depositTxn.toAccount, response.transactions[0].to)
    assertEquals(depositTxn.memo, response.transactions[0].depositMemo)
    assertEquals(depositTxn.memoType, response.transactions[0].depositMemoType)
    assertEquals(depositTxn.startedAt.toString(), response.transactions[0].startedAt)
    assertEquals(depositTxn.updatedAt.toString(), response.transactions[0].updatedAt)
    assertEquals(depositTxn.completedAt.toString(), response.transactions[0].completedAt)
    assertEquals(depositTxn.stellarTransactionId, response.transactions[0].stellarTransactionId)
    assertEquals(depositTxn.externalTransactionId, response.transactions[0].externalTransactionId)
    assertEquals(depositTxn.message, response.transactions[0].message)
    assertEquals(depositTxn.requiredInfoMessage, response.transactions[0].requiredInfoMessage)
    assertEquals(depositTxn.requiredInfoUpdates, response.transactions[0].requiredInfoUpdates)

    assertEquals(depositTxn.refunds.amountFee, response.transactions[0].refunds.amountFee)
    assertEquals(depositTxn.refunds.amountRefunded, response.transactions[0].refunds.amountRefunded)
    assertEquals(depositTxn.refunds.payments.size, response.transactions[0].refunds.payments.size)
    assertEquals(depositTxn.refunds.payments[0].id, response.transactions[0].refunds.payments[0].id)
    assertEquals(
      depositTxn.refunds.payments[0].idType,
      response.transactions[0].refunds.payments[0].idType
    )
    assertEquals(
      depositTxn.refunds.payments[0].amount,
      response.transactions[0].refunds.payments[0].amount
    )
    assertEquals(
      depositTxn.refunds.payments[0].fee,
      response.transactions[0].refunds.payments[0].fee
    )
  }

  private fun createDepositTxn(
    sep10Account: String,
    sep10AccountMemo: String? = null
  ): Sep6Transaction {
    val txn = PojoSep6Transaction()

    val payment = PojoSep6RefundPayment()
    payment.id = "refund-payment-id"
    payment.idType = "refund-payment-id-type"
    payment.amount = "100"
    payment.fee = "0"

    val refunds = PojoSep6Refunds()
    refunds.amountRefunded = "0"
    refunds.amountFee = "0"
    refunds.payments = listOf(payment)

    txn.id = UUID.randomUUID().toString()
    txn.kind = "deposit"
    txn.status = "complete"
    txn.statusEta = 5
    txn.moreInfoUrl = "https://example.com/more_info"
    txn.amountIn = "100"
    txn.amountInAsset = "USD"
    txn.amountOut = "98"
    txn.amountOutAsset = "USDC"
    txn.amountFee = "2"
    txn.amountOutAsset = "stellar:USDC:GABCD"
    txn.sep10Account = sep10Account
    txn.sep10AccountMemo = sep10AccountMemo
    txn.fromAccount = "GABCD"
    txn.toAccount = "GABCD"
    txn.memo = "some memo"
    txn.memoType = "text"
    txn.startedAt = Instant.now()
    txn.updatedAt = Instant.now()
    txn.completedAt = Instant.now()
    txn.stellarTransactionId = "stellar-id"
    txn.externalTransactionId = "external-id"
    txn.message = "some message"
    txn.refunds = refunds
    txn.requiredInfoMessage = "some info message"
    txn.requiredInfoUpdates = "some info updates"

    return txn
  }
}
