package org.stellar.anchor.sep6

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
import org.skyscreamer.jsonassert.JSONAssert
import org.stellar.anchor.TestConstants.Companion.TEST_ACCOUNT
import org.stellar.anchor.TestConstants.Companion.TEST_ASSET
import org.stellar.anchor.TestHelper
import org.stellar.anchor.api.exception.NotFoundException
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.api.exception.SepValidationException
import org.stellar.anchor.api.sep.sep6.GetTransactionRequest
import org.stellar.anchor.api.sep.sep6.GetTransactionsRequest
import org.stellar.anchor.api.sep.sep6.InfoResponse
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.RefundPayment
import org.stellar.anchor.api.shared.Refunds
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.config.Sep6Config
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

  val transactionsJson =
    """
      {
          "transactions": [
              {
                  "id": "2cb630d3-030b-4a0e-9d9d-f26b1df25d12",
                  "kind": "deposit",
                  "status": "complete",
                  "status_eta": 5,
                  "more_info_url": "https://example.com/more_info",
                  "amount_in": "100",
                  "amount_in_asset": "USD",
                  "amount_out": "98",
                  "amount_out_asset": "stellar:USDC:GABCD",
                  "amount_fee": "2",
                  "from": "GABCD",
                  "to": "GABCD",
                  "depositMemo": "some memo",
                  "depositMemoType": "text",
                  "started_at": "2023-08-01T16:53:20Z",
                  "updated_at": "2023-08-01T16:53:20Z",
                  "completed_at": "2023-08-01T16:53:20Z",
                  "stellar_transaction_id": "stellar-id",
                  "external_transaction_id": "external-id",
                  "message": "some message",
                  "refunds": {
                      "amount_refunded": {
                          "amount": "100",
                          "asset": "USD"
                      },
                      "amount_fee": {
                          "amount": "0",
                          "asset": "USD"
                      },
                      "payments": [
                          {
                              "id": "refund-payment-id",
                              "id_type": "external",
                              "amount": {
                                  "amount": "100",
                                  "asset": "USD"
                              },
                              "fee": {
                                  "amount": "0",
                                  "asset": "USD"
                              }
                          }
                      ]
                  },
                  "required_info_message": "some info message",
                  "required_info_updates": "some info updates"
              }
          ]
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

    JSONAssert.assertEquals(transactionsJson, gson.toJson(response), true)
  }

  private fun createDepositTxn(
    sep10Account: String,
    sep10AccountMemo: String? = null
  ): Sep6Transaction {
    val txn = PojoSep6Transaction()

    val payment = RefundPayment()
    payment.id = "refund-payment-id"
    payment.idType = RefundPayment.IdType.EXTERNAL
    payment.amount = Amount.create("100", "USD")
    payment.fee = Amount.create("0", "USD")

    val refunds = Refunds()
    refunds.amountRefunded = Amount.create("100", "USD")
    refunds.amountFee = Amount.create("0", "USD")
    refunds.payments = arrayOf(payment)

    txn.id = "2cb630d3-030b-4a0e-9d9d-f26b1df25d12"
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
    txn.startedAt = Instant.ofEpochMilli(1690908800000L)
    txn.updatedAt = Instant.ofEpochMilli(1690908800000L)
    txn.completedAt = Instant.ofEpochMilli(1690908800000L)
    txn.stellarTransactionId = "stellar-id"
    txn.externalTransactionId = "external-id"
    txn.message = "some message"
    txn.refunds = refunds
    txn.requiredInfoMessage = "some info message"
    txn.requiredInfoUpdates = "some info updates"

    return txn
  }
}
