package org.stellar.anchor.sep6

import com.google.gson.Gson
import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.TestConstants.Companion.TEST_ACCOUNT
import org.stellar.anchor.TestConstants.Companion.TEST_ASSET
import org.stellar.anchor.TestConstants.Companion.TEST_ASSET_SEP38_FORMAT
import org.stellar.anchor.TestConstants.Companion.TEST_QUOTE_ID
import org.stellar.anchor.TestHelper
import org.stellar.anchor.api.event.AnchorEvent
import org.stellar.anchor.api.exception.NotFoundException
import org.stellar.anchor.api.exception.SepNotAuthorizedException
import org.stellar.anchor.api.exception.SepValidationException
import org.stellar.anchor.api.sep.sep6.*
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.api.shared.RefundPayment
import org.stellar.anchor.api.shared.Refunds
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.config.Sep6Config
import org.stellar.anchor.event.EventService
import org.stellar.anchor.sep6.ExchangeAmountsCalculator.Amounts
import org.stellar.anchor.util.GsonUtils

class Sep6ServiceTest {
  companion object {
    val gson: Gson = GsonUtils.getInstance()
  }

  private val assetService: AssetService = DefaultAssetService.fromJsonResource("test_assets.json")

  @MockK(relaxed = true) lateinit var sep6Config: Sep6Config
  @MockK(relaxed = true) lateinit var txnStore: Sep6TransactionStore
  @MockK(relaxed = true) lateinit var exchangeAmountsCalculator: ExchangeAmountsCalculator
  @MockK(relaxed = true) lateinit var eventService: EventService
  @MockK(relaxed = true) lateinit var eventSession: EventService.Session

  private lateinit var sep6Service: Sep6Service

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { sep6Config.features.isAccountCreation } returns false
    every { sep6Config.features.isClaimableBalances } returns false
    every { txnStore.newInstance() } returns PojoSep6Transaction()
    every { eventService.createSession(any(), any()) } returns eventSession
    sep6Service =
      Sep6Service(sep6Config, assetService, txnStore, exchangeAmountsCalculator, eventService)
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
                      "cash": {
                          "fields": {}
                      },
                      "bank_account": {
                          "fields": {}
                      }
                  }
              }
          },
          "withdraw-exchange": {
              "USDC": {
                  "enabled": true,
                  "authentication_required": true,
                  "types": {
                      "cash": {
                          "fields": {}
                      },
                      "bank_account": {
                          "fields": {}
                      }
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
                  "deposit_memo": "some memo",
                  "deposit_memo_type": "text",
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
                  "required_info_updates": ["first_name", "last_name"]
              }
          ]
      }
    """
      .trimIndent()

  val depositTxnJson =
    """
      {
          "status": "incomplete",
          "kind": "deposit",
          "type": "bank_account",
          "requestAssetCode": "USDC",
          "requestAssetIssuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountExpected": "100",
          "sep10Account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "toAccount": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO"
      }
    """
      .trimIndent()

  val depositTxnEventJson =
    """
      {
          "type": "transaction_created",
          "sep": "6",
          "transaction": {
              "sep": "6",
              "kind": "deposit",
              "status": "incomplete",
              "amount_expected": {
                  "amount": "100",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "destination_account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO"
          }
      }
    """
      .trimIndent()

  val withdrawResJson =
    """
      {
          "account_id": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
          "memo_type": "hash"
      }
    """
      .trimIndent()

  val withdrawTxnJson =
    """
      {
          "status": "incomplete",
          "kind": "withdrawal",
          "type": "bank_account",
          "requestAssetCode": "USDC",
          "requestAssetIssuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountExpected": "100",
          "sep10Account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "withdrawAnchorAccount": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
          "fromAccount": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "toAccount": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
          "memoType": "hash",
          "refundMemo": "some text",
          "refundMemoType": "text"
      }
    """
      .trimIndent()

  val withdrawTxnEventJson =
    """
      {
          "type": "transaction_created",
          "sep": "6",
          "transaction": {
              "sep": "6",
              "kind": "withdrawal",
              "status": "incomplete",
              "amount_expected": {
                  "amount": "100",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "source_account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
              "destination_account": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
              "memo_type": "hash",
              "refund_memo": "some text",
              "refund_memo_type": "text"
          }
      }
    """
      .trimIndent()

  val withdrawExchangeTxnJson =
    """
      {
          "status": "incomplete",
          "kind": "withdrawal-exchange",
          "type": "bank_account",
          "requestAssetCode": "USDC",
          "requestAssetIssuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountIn": "100",
          "amountInAsset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountOut": "98",
          "amountOutAsset": "iso4217:USD",
          "amountFee": "2",
          "amountFeeAsset": "iso4217:USD",
          "amountExpected": "100",
          "sep10Account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "withdrawAnchorAccount": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
          "fromAccount": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "memoType": "hash",
          "quoteId": "test-quote-id",
          "refundMemo": "some text",
          "refundMemoType": "text"
      }
    """
      .trimIndent()

  val withdrawExchangeTxnEventJson =
    """
      {
          "type": "transaction_created",
          "sep": "6",
          "transaction": {
              "sep": "6",
              "kind": "withdrawal-exchange",
              "status": "incomplete",
              "amount_expected": {
                  "amount": "100",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "amount_in": {
                  "amount": "100",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "amount_out": {
                  "amount": "98",
                  "asset": "iso4217:USD"
              },
              "amount_fee": {
                  "amount": "2",
                  "asset": "iso4217:USD"
              },
              "quote_id": "test-quote-id",
              "source_account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
              "memo_type": "hash",
              "refund_memo": "some text",
              "refund_memo_type": "text"
          }
      }
    """
      .trimIndent()

  val withdrawExchangeTxnWithoutQuoteJson =
    """
      {
          "status": "incomplete",
          "kind": "withdrawal-exchange",
          "type": "bank_account",
          "requestAssetCode": "USDC",
          "requestAssetIssuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountIn": "100",
          "amountInAsset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "amountOut": "98",
          "amountOutAsset": "iso4217:USD",
          "amountFee": "2",
          "amountFeeAsset": "iso4217:USD",
          "amountExpected": "100",
          "sep10Account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "withdrawAnchorAccount": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
          "fromAccount": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
          "memoType": "hash",
          "refundMemo": "some text",
          "refundMemoType": "text"
      }
    """
      .trimIndent()

  val withdrawExchangeTxnWithoutQuoteEventJson =
    """
      {
          "type": "transaction_created",
          "sep": "6",
          "transaction": {
              "sep": "6",
              "kind": "withdrawal-exchange",
              "status": "incomplete",
              "amount_expected": {
                  "amount": "100",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "amount_in": {
                  "amount": "100",
                  "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
              },
              "amount_out": {
                  "amount": "98",
                  "asset": "iso4217:USD"
              },
              "amount_fee": {
                  "amount": "2",
                  "asset": "iso4217:USD"
              },
              "source_account": "GBLGJA4TUN5XOGTV6WO2BWYUI2OZR5GYQ5PDPCRMQ5XEPJOYWB2X4CJO",
              "memo_type": "hash",
              "refund_memo": "some text",
              "refund_memo_type": "text"
          }
      }
    """
      .trimIndent()

  @Test
  fun `test INFO response`() {
    val infoResponse = sep6Service.info
    println(gson.toJson(infoResponse))
    assertEquals(gson.fromJson(infoJson, InfoResponse::class.java), infoResponse)
  }

  @Test
  fun `test deposit`() {
    val slotTxn = slot<Sep6Transaction>()
    every { txnStore.save(capture(slotTxn)) } returns null

    val slotEvent = slot<AnchorEvent>()
    every { eventSession.publish(capture(slotEvent)) } returns Unit

    val request =
      StartDepositRequest.builder()
        .assetCode(TEST_ASSET)
        .account(TEST_ACCOUNT)
        .type("bank_account")
        .amount("100")
        .build()
    val response = sep6Service.deposit(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)

    // Verify effects
    verify(exactly = 1) { txnStore.save(any()) }
    verify(exactly = 1) { eventSession.publish(any()) }

    JSONAssert.assertEquals(depositTxnJson, gson.toJson(slotTxn.captured), JSONCompareMode.LENIENT)
    assert(slotTxn.captured.id.isNotEmpty())
    assertNotNull(slotTxn.captured.startedAt)

    JSONAssert.assertEquals(
      depositTxnEventJson,
      gson.toJson(slotEvent.captured),
      JSONCompareMode.LENIENT
    )
    assert(slotEvent.captured.id.isNotEmpty())
    assert(slotEvent.captured.transaction.id.isNotEmpty())
    assertNotNull(slotEvent.captured.transaction.startedAt)

    // Verify response
    assertEquals(slotTxn.captured.id, response.id)
  }

  @Test
  fun `test deposit with unsupported asset`() {
    val request =
      StartDepositRequest.builder()
        .assetCode("??")
        .account(TEST_ACCOUNT)
        .type("bank_account")
        .amount("100")
        .build()

    assertThrows<SepValidationException> {
      sep6Service.deposit(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }
    verify { txnStore wasNot Called }
    verify { eventSession wasNot Called }
  }

  @Test
  fun `test deposit does not send event if transaction fails to save`() {
    every { txnStore.save(any()) } throws RuntimeException("unexpected failure")

    val slotEvent = slot<AnchorEvent>()
    every { eventSession.publish(capture(slotEvent)) } returns Unit

    val request =
      StartDepositRequest.builder()
        .assetCode(TEST_ASSET)
        .account(TEST_ACCOUNT)
        .type("bank_account")
        .amount("100")
        .build()
    assertThrows<java.lang.RuntimeException> {
      sep6Service.deposit(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }

    // Verify effects
    verify(exactly = 1) { txnStore.save(any()) }
    verify { eventSession wasNot called }
  }

  @Test
  fun `test withdraw`() {
    val slotTxn = slot<Sep6Transaction>()
    every { txnStore.save(capture(slotTxn)) } returns null

    val slotEvent = slot<AnchorEvent>()
    every { eventSession.publish(capture(slotEvent)) } returns Unit

    val request =
      StartWithdrawRequest.builder()
        .assetCode(TEST_ASSET)
        .type("bank_account")
        .amount("100")
        .refundMemo("some text")
        .refundMemoType("text")
        .build()

    val response = sep6Service.withdraw(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)

    // Verify effects
    verify(exactly = 1) { txnStore.save(any()) }
    verify(exactly = 1) { eventSession.publish(any()) }

    JSONAssert.assertEquals(withdrawTxnJson, gson.toJson(slotTxn.captured), JSONCompareMode.LENIENT)
    assert(slotTxn.captured.id.isNotEmpty())
    assert(slotTxn.captured.memo.isNotEmpty())
    assertEquals(slotTxn.captured.memoType, "hash")
    assertNotNull(slotTxn.captured.startedAt)

    JSONAssert.assertEquals(
      withdrawTxnEventJson,
      gson.toJson(slotEvent.captured),
      JSONCompareMode.LENIENT
    )
    assert(slotEvent.captured.id.isNotEmpty())
    assert(slotEvent.captured.transaction.id.isNotEmpty())
    assert(slotEvent.captured.transaction.memo.isNotEmpty())
    assertEquals(slotEvent.captured.transaction.memoType, "hash")
    assertNotNull(slotEvent.captured.transaction.startedAt)

    // Verify response
    assertEquals(slotTxn.captured.id, response.id)
    assertEquals(slotTxn.captured.memo, response.memo)
    JSONAssert.assertEquals(withdrawResJson, gson.toJson(response), JSONCompareMode.LENIENT)
  }

  @Test
  fun `test withdraw with unsupported asset`() {
    val request =
      StartWithdrawRequest.builder().assetCode("??").type("bank_account").amount("100").build()

    assertThrows<SepValidationException> {
      sep6Service.withdraw(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }
    verify { txnStore wasNot Called }
    verify { eventSession wasNot Called }
  }

  @Test
  fun `test withdraw with unsupported type`() {
    val request =
      StartWithdrawRequest.builder()
        .assetCode(TEST_ASSET)
        .type("unsupported_Type")
        .amount("100")
        .build()

    assertThrows<SepValidationException> {
      sep6Service.withdraw(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }
    verify { txnStore wasNot Called }
    verify { eventSession wasNot Called }
  }

  @ValueSource(strings = ["0", "-1", "0.0", "0.0000000001"])
  @ParameterizedTest
  fun `test withdraw with bad amount`(amount: String) {
    val request =
      StartWithdrawRequest.builder()
        .assetCode(TEST_ASSET)
        .type("bank_account")
        .amount(amount)
        .build()

    assertThrows<SepValidationException> {
      sep6Service.withdraw(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }
    verify { txnStore wasNot Called }
    verify { eventSession wasNot Called }
  }

  @Test
  fun `test withdraw does not send event if transaction fails to save`() {
    every { txnStore.save(any()) } throws RuntimeException("unexpected failure")

    val slotEvent = slot<AnchorEvent>()
    every { eventSession.publish(capture(slotEvent)) } returns Unit

    val request =
      StartWithdrawRequest.builder()
        .assetCode(TEST_ASSET)
        .type("bank_account")
        .amount("100")
        .build()
    assertThrows<java.lang.RuntimeException> {
      sep6Service.withdraw(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }

    // Verify effects
    verify(exactly = 1) { txnStore.save(any()) }
    verify { eventSession wasNot called }
  }

  @Test
  fun `test withdraw-exchange with quote`() {
    val sourceAsset = TEST_ASSET
    val destinationAsset = "iso4217:USD"

    val slotTxn = slot<Sep6Transaction>()
    every { txnStore.save(capture(slotTxn)) } returns null

    val slotEvent = slot<AnchorEvent>()
    every { eventSession.publish(capture(slotEvent)) } returns Unit

    every { exchangeAmountsCalculator.calculateFromQuote(TEST_QUOTE_ID, any(), any()) } returns
      Amounts.builder()
        .amountIn("100")
        .amountInAsset(TEST_ASSET_SEP38_FORMAT)
        .amountOut("98")
        .amountOutAsset(destinationAsset)
        .amountFee("2")
        .amountFeeAsset(destinationAsset)
        .build()

    val request =
      StartWithdrawExchangeRequest.builder()
        .sourceAsset(sourceAsset)
        .destinationAsset(destinationAsset)
        .quoteId(TEST_QUOTE_ID)
        .type("bank_account")
        .amount("100")
        .refundMemo("some text")
        .refundMemoType("text")
        .build()

    val response = sep6Service.withdrawExchange(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)

    // Verify effects
    verify(exactly = 1) {
      exchangeAmountsCalculator.calculateFromQuote(TEST_QUOTE_ID, any(), "100")
    }
    verify(exactly = 1) { txnStore.save(any()) }
    verify(exactly = 1) { eventSession.publish(any()) }

    JSONAssert.assertEquals(
      withdrawExchangeTxnJson,
      gson.toJson(slotTxn.captured),
      JSONCompareMode.LENIENT
    )
    assert(slotTxn.captured.id.isNotEmpty())
    assert(slotTxn.captured.memo.isNotEmpty())
    assertEquals(slotTxn.captured.memoType, "hash")
    assertNotNull(slotTxn.captured.startedAt)

    JSONAssert.assertEquals(
      withdrawExchangeTxnEventJson,
      gson.toJson(slotEvent.captured),
      JSONCompareMode.LENIENT
    )
    assert(slotEvent.captured.id.isNotEmpty())
    assert(slotEvent.captured.transaction.id.isNotEmpty())
    assert(slotEvent.captured.transaction.memo.isNotEmpty())
    assertEquals(slotEvent.captured.transaction.memoType, "hash")
    assertNotNull(slotEvent.captured.transaction.startedAt)

    // Verify response
    assertEquals(slotTxn.captured.id, response.id)
    assertEquals(slotTxn.captured.memo, response.memo)
    JSONAssert.assertEquals(withdrawResJson, gson.toJson(response), JSONCompareMode.LENIENT)
  }

  @Test
  fun `test withdraw-exchange without quote`() {
    val sourceAsset = TEST_ASSET
    val destinationAsset = "iso4217:USD"

    val slotTxn = slot<Sep6Transaction>()
    every { txnStore.save(capture(slotTxn)) } returns null

    val slotEvent = slot<AnchorEvent>()
    every { eventSession.publish(capture(slotEvent)) } returns Unit

    every { exchangeAmountsCalculator.calculate(any(), any(), "100", TEST_ACCOUNT) } returns
      Amounts.builder()
        .amountIn("100")
        .amountInAsset(TEST_ASSET_SEP38_FORMAT)
        .amountOut("98")
        .amountOutAsset(destinationAsset)
        .amountFee("2")
        .amountFeeAsset(destinationAsset)
        .build()

    val request =
      StartWithdrawExchangeRequest.builder()
        .sourceAsset(sourceAsset)
        .destinationAsset(destinationAsset)
        .type("bank_account")
        .amount("100")
        .refundMemo("some text")
        .refundMemoType("text")
        .build()

    val response = sep6Service.withdrawExchange(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)

    // Verify effects
    verify(exactly = 1) { exchangeAmountsCalculator.calculate(any(), any(), "100", TEST_ACCOUNT) }
    verify(exactly = 1) { txnStore.save(any()) }
    verify(exactly = 1) { eventSession.publish(any()) }

    JSONAssert.assertEquals(
      withdrawExchangeTxnWithoutQuoteJson,
      gson.toJson(slotTxn.captured),
      JSONCompareMode.LENIENT
    )
    assert(slotTxn.captured.id.isNotEmpty())
    assert(slotTxn.captured.memo.isNotEmpty())
    assertEquals(slotTxn.captured.memoType, "hash")
    assertNotNull(slotTxn.captured.startedAt)

    JSONAssert.assertEquals(
      withdrawExchangeTxnWithoutQuoteEventJson,
      gson.toJson(slotEvent.captured),
      JSONCompareMode.LENIENT
    )
    assert(slotEvent.captured.id.isNotEmpty())
    assert(slotEvent.captured.transaction.id.isNotEmpty())
    assert(slotEvent.captured.transaction.memo.isNotEmpty())
    assertEquals(slotEvent.captured.transaction.memoType, "hash")
    assertNotNull(slotEvent.captured.transaction.startedAt)

    // Verify response
    assertEquals(slotTxn.captured.id, response.id)
    assertEquals(slotTxn.captured.memo, response.memo)
    JSONAssert.assertEquals(withdrawResJson, gson.toJson(response), JSONCompareMode.LENIENT)
  }

  @Test
  fun `test withdraw-exchange with unsupported source asset`() {
    val request =
      StartWithdrawExchangeRequest.builder()
        .sourceAsset("???")
        .destinationAsset("iso4217:USD")
        .type("bank_account")
        .amount("100")
        .build()

    assertThrows<SepValidationException> {
      sep6Service.withdrawExchange(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }
    verify { exchangeAmountsCalculator wasNot Called }
    verify { txnStore wasNot Called }
    verify { eventSession wasNot Called }
  }

  @Test
  fun `test withdraw-exchange with unsupported destination asset`() {
    val request =
      StartWithdrawExchangeRequest.builder()
        .sourceAsset(TEST_ASSET)
        .destinationAsset("USD")
        .type("bank_account")
        .amount("100")
        .build()

    assertThrows<SepValidationException> {
      sep6Service.withdrawExchange(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }
    verify { exchangeAmountsCalculator wasNot Called }
    verify { txnStore wasNot Called }
    verify { eventSession wasNot Called }
  }

  @Test
  fun `test withdraw-exchange with unsupported type`() {
    val request =
      StartWithdrawExchangeRequest.builder()
        .sourceAsset(TEST_ASSET)
        .destinationAsset("iso4217:USD")
        .type("unsupported_Type")
        .amount("100")
        .build()

    assertThrows<SepValidationException> {
      sep6Service.withdrawExchange(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }
    verify { exchangeAmountsCalculator wasNot Called }
    verify { txnStore wasNot Called }
    verify { eventSession wasNot Called }
  }

  @ValueSource(strings = ["0", "-1", "0.0", "0.0000000001"])
  @ParameterizedTest
  fun `test withdraw-exchange with bad amount`(amount: String) {
    val request =
      StartWithdrawExchangeRequest.builder()
        .sourceAsset(TEST_ASSET)
        .destinationAsset("iso4217:USD")
        .type("bank_account")
        .amount(amount)
        .build()

    assertThrows<SepValidationException> {
      sep6Service.withdrawExchange(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }
    verify { exchangeAmountsCalculator wasNot Called }
    verify { txnStore wasNot Called }
    verify { eventSession wasNot Called }
  }

  @Test
  fun `test withdraw-exchange does not send event if transaction fails to save`() {
    every { txnStore.save(any()) } throws RuntimeException("unexpected failure")

    val slotEvent = slot<AnchorEvent>()
    every { eventSession.publish(capture(slotEvent)) } returns Unit

    val request =
      StartWithdrawExchangeRequest.builder()
        .sourceAsset(TEST_ASSET)
        .destinationAsset("iso4217:USD")
        .type("bank_account")
        .amount("100")
        .build()
    assertThrows<java.lang.RuntimeException> {
      sep6Service.withdrawExchange(TestHelper.createSep10Jwt(TEST_ACCOUNT), request)
    }

    // Verify effects
    verify(exactly = 1) { txnStore.save(any()) }
    verify { eventSession wasNot called }
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
    txn.requiredInfoUpdates = listOf("first_name", "last_name")

    return txn
  }
}
