package org.stellar.anchor.sep31

import com.google.gson.Gson
import io.mockk.*
import io.mockk.impl.annotations.MockK
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.stellar.anchor.Constants
import org.stellar.anchor.TestHelper
import org.stellar.anchor.api.callback.CustomerIntegration
import org.stellar.anchor.api.callback.FeeIntegration
import org.stellar.anchor.api.callback.GetFeeResponse
import org.stellar.anchor.api.exception.AnchorException
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.SepValidationException
import org.stellar.anchor.api.sep.AssetInfo
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerRequest
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerResponse
import org.stellar.anchor.api.sep.sep31.Sep31DepositInfo
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest.Sep31TxnFields
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionResponse
import org.stellar.anchor.api.sep.sep38.RateFee
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.ResourceJsonAssetService
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.config.Sep31Config
import org.stellar.anchor.config.Sep31Config.PaymentType.STRICT_RECEIVE
import org.stellar.anchor.config.Sep31Config.PaymentType.STRICT_SEND
import org.stellar.anchor.event.EventPublishService
import org.stellar.anchor.event.models.StellarId
import org.stellar.anchor.event.models.TransactionEvent
import org.stellar.anchor.sep31.Sep31Service.Sep31CustomerInfoNeededException
import org.stellar.anchor.sep31.Sep31Service.Sep31MissingFieldException
import org.stellar.anchor.sep38.PojoSep38Quote
import org.stellar.anchor.sep38.Sep38QuoteStore
import org.stellar.anchor.util.GsonUtils

class Sep31ServiceTest {
  companion object {
    val gson: Gson = GsonUtils.getInstance()

    private const val stellarUSDC =
      "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"

    private const val stellarJPYC =
      "stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"

    private const val requestJson =
      """
        {
          "amount": "500",
          "asset_code": "USDC",
          "sender_id": "3f720570-26bd-4ea6-b0a2-1643ef4149da",
          "receiver_id": "69401a9a-7edb-4121-b507-1089a4389b11",
          "fields": {
            "transaction": {
              "receiver_account_number": "1",
              "type": "SWIFT",
              "receiver_routing_number": "1"
            }
          }
        }
    """

    private const val feeJson =
      """
        {
          "amount": "2",
          "asset": "USDC"
        }
    """

    private const val assetJson =
      """
            {
              "code": "USDC",
              "issuer": "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
              "distribution_account": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
              "schema": "stellar",
              "significant_decimals": 2,
              "deposit": {
                "enabled": true,
                "fee_fixed": 0,
                "fee_percent": 0,
                "min_amount": 1,
                "max_amount": 1000000,
                "fee_minimum": 0
              },
              "withdraw": {
                "enabled": false,
                "fee_fixed": 0,
                "fee_percent": 0,
                "min_amount": 1,
                "max_amount": 1000000
              },
              "send": {
                "fee_fixed": 0,
                "fee_percent": 0,
                "min_amount": 1,
                "max_amount": 1000000
              },
              "sep31": {
                "quotes_supported": true,
                "quotes_required": true,
                "sep12": {
                  "sender": {
                    "types": {
                      "sep31-sender": {
                        "description": "U.S. citizens limited to sending payments of less than ${'$'}10,000 in value"
                      },
                      "sep31-large-sender": {
                        "description": "U.S. citizens that do not have sending limits"
                      },
                      "sep31-foreign-sender": {
                        "description": "non-U.S. citizens sending payments of less than ${'$'}10,000 in value"
                      }
                    }
                  },
                  "receiver": {
                    "types": {
                      "sep31-receiver": {
                        "description": "U.S. citizens receiving USD"
                      },
                      "sep31-foreign-receiver": {
                        "description": "non-U.S. citizens receiving USD"
                      }
                    }
                  }
                },
                "fields": {
                  "transaction": {
                    "receiver_routing_number": {
                      "description": "routing number of the destination bank account",
                      "optional": false
                    },
                    "receiver_account_number": {
                      "description": "bank account number of the destination",
                      "optional": false
                    },
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
              "sep38": {
                "exchangeable_assets": [
                  "iso4217:USD"
                ]
              },
              "sep31_enabled": true,
              "sep38_enabled": true
            }

        """

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

    private const val quoteJson =
      """
      {
        "id": "de762cda-a193-4961-861e-57b31fed6eb3",
        "expires_at": "2021-04-30T07:42:23",
        "total_price": "0.008",
        "price": "0.0072",
        "sell_asset": "stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN",
        "sell_amount": "100",
        "buy_asset": "stellar:JPYC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
        "buy_amount": "12500",
        "fee": {
          "total": "10.00",
          "asset": "stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN"
        }
      }
  """
  }

  private val assetService: AssetService = ResourceJsonAssetService("test_assets.json")

  @MockK(relaxed = true) private lateinit var txnStore: Sep31TransactionStore

  @MockK(relaxed = true) lateinit var appConfig: AppConfig
  @MockK(relaxed = true) lateinit var sep31Config: Sep31Config
  @MockK(relaxed = true) lateinit var sep31DepositInfoGenerator: Sep31DepositInfoGenerator
  @MockK(relaxed = true) lateinit var quoteStore: Sep38QuoteStore
  @MockK(relaxed = true) lateinit var feeIntegration: FeeIntegration
  @MockK(relaxed = true) lateinit var customerIntegration: CustomerIntegration
  @MockK(relaxed = true) lateinit var eventPublishService: EventPublishService

  private lateinit var jwtService: JwtService
  private lateinit var sep31Service: Sep31Service

  private lateinit var request: Sep31PostTransactionRequest
  private lateinit var txn: Sep31Transaction
  private lateinit var fee: Amount
  private lateinit var asset: AssetInfo
  private lateinit var quote: PojoSep38Quote

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { appConfig.stellarNetworkPassphrase } returns Constants.TEST_NETWORK_PASS_PHRASE
    every { appConfig.hostUrl } returns Constants.TEST_HOST_URL
    every { appConfig.jwtSecretKey } returns Constants.TEST_JWT_SECRET
    every { appConfig.languages } returns listOf("en")
    every { sep31Config.paymentType } returns STRICT_SEND
    every { txnStore.newTransaction() } returns PojoSep31Transaction()
    jwtService = spyk(JwtService(appConfig))

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
        eventPublishService,
      )

    request = gson.fromJson(requestJson, Sep31PostTransactionRequest::class.java)
    txn = gson.fromJson(txnJson, PojoSep31Transaction::class.java)
    fee = gson.fromJson(feeJson, Amount::class.java)
    asset = gson.fromJson(assetJson, AssetInfo::class.java)
    quote = gson.fromJson(quoteJson, PojoSep38Quote::class.java)
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
  }
  
  @Test
  fun test_updateTxAmountsWhenNoQuoteWasUsed() {
    Sep31Service.Context.get().setTransaction(txn)
    Sep31Service.Context.get().setRequest(request)
    Sep31Service.Context.get().setFee(fee)
    Sep31Service.Context.get().setAsset(asset)
    every { sep31Config.paymentType } returns STRICT_SEND

    request.amount = "100"
    fee.amount = "2"
    sep31Service.updateTxAmountsWhenNoQuoteWasUsed()
    assertEquals(txn.amountIn, "100")
    assertEquals(txn.amountOut, "98")

    every { sep31Config.paymentType } returns STRICT_RECEIVE
    sep31Service.updateTxAmountsWhenNoQuoteWasUsed()
    assertEquals("102", txn.amountIn)
    assertEquals("100", txn.amountOut)
  }

  @Test
  fun test_quotesSupportedAndRequiredValidation() {
    val assetServiceQuotesNotSupported: AssetService =
      ResourceJsonAssetService(
        "test_assets.json.quotes_required_but_not_supported",
      )
    val ex: AnchorException = assertThrows {
      Sep31Service(
        appConfig,
        sep31Config,
        txnStore,
        sep31DepositInfoGenerator,
        quoteStore,
        assetServiceQuotesNotSupported,
        feeIntegration,
        customerIntegration,
        eventPublishService,
      )
    }
    assertInstanceOf(SepValidationException::class.java, ex)
    assertEquals(
      "if quotes_required is true, quotes_supported must also be true",
      ex.message,
    )
  }

  @Test
  fun test_updateTxAmountsBasedOnQuote() {
    Sep31Service.Context.get().setTransaction(txn)
    Sep31Service.Context.get().setRequest(request)
    Sep31Service.Context.get().setFee(fee)
    Sep31Service.Context.get().setQuote(quote)

    // Fee is as sell asset
    every { sep31Config.paymentType } throws Exception("paymentType must not be called")
    request.quoteId = "quote_id"
    request.amount = "100"
    request.assetCode = "USDC"
    sep31Service.updateTxAmountsBasedOnQuote()

    // TODO: Add fee validation.
    assertEquals("100", txn.amountIn)
    assertEquals(
      "stellar:USDC:GA5ZSEJYB37JRC5AVCIA5MOP4RHTM335X2KGX3IHOJAPP5RE34K4KZVN",
      txn.amountInAsset,
    )
    assertEquals("12500", txn.amountOut)
    assertEquals(stellarJPYC, txn.amountOutAsset)
  }

  @Test
  fun test_postTransaction_failure() {
    val jwtToken = TestHelper.createJwtToken()

    // missing asset code
    val postTxRequest = Sep31PostTransactionRequest()
    var ex: AnchorException = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("asset null:null is not supported.", ex.message)

    // invalid asset code
    postTxRequest.assetCode = "FOO"
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("asset FOO:null is not supported.", ex.message)

    // invalid asset issuer
    postTxRequest.assetCode = "USDC"
    postTxRequest.assetIssuer = "BAR"
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("asset USDC:BAR is not supported.", ex.message)

    // missing amount
    postTxRequest.assetIssuer = null
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("amount cannot be empty", ex.message)

    // invalid amount (not a number)
    postTxRequest.amount = "FOO BAR"
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("amount is invalid", ex.message)

    // invalid amount (not a positive number)
    postTxRequest.amount = "-0.01"
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("amount should be positive", ex.message)

    // invalid amount (not a positive number)
    postTxRequest.amount = "0"
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("amount should be positive", ex.message)

    // missing required fields
    postTxRequest.lang = "en"
    postTxRequest.amount = "1"
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("'fields' field cannot be empty", ex.message)

    // missing required fields.transaction
    postTxRequest.fields = Sep31TxnFields(null)
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("'fields' field must have one 'transaction' field", ex.message)

    // missing fields [receiver_routing_number, receiver_account_number, type]
    postTxRequest.fields = Sep31TxnFields(hashMapOf())
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(Sep31MissingFieldException::class.java, ex)
    val wantMissingFieldsNames =
      listOf("receiver_account_number", "type", "receiver_routing_number")
    val gotMissingFieldsNames = (ex as Sep31MissingFieldException).missingFields.transaction.keys
    assertTrue(
      wantMissingFieldsNames.containsAll(gotMissingFieldsNames),
      "missing field names don't match",
    )
    assertTrue(
      gotMissingFieldsNames.containsAll(wantMissingFieldsNames),
      "missing field names don't match",
    )

    // missing receiver_id
    val fields =
      hashMapOf(
        "receiver_account_number" to "1",
        "type" to "1",
        "receiver_routing_number" to "SWIFT",
      )
    postTxRequest.fields = Sep31TxnFields(fields)
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("receiver_id cannot be empty.", ex.message)

    // not found receiver_id
    every { customerIntegration.getCustomer(any()) } returns null
    postTxRequest.receiverId = "receiver_foo"
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(Sep31CustomerInfoNeededException::class.java, ex)
    assertEquals("sep31-receiver", (ex as Sep31CustomerInfoNeededException).type)

    // missing sender_id
    val receiverId = "137938d4-43a7-4252-a452-842adcee474c"
    postTxRequest.receiverId = receiverId
    var request = Sep12GetCustomerRequest.builder().id(receiverId).type("sep31-receiver").build()
    every { customerIntegration.getCustomer(request) } returns Sep12GetCustomerResponse()
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sender_id cannot be empty.", ex.message)

    // not found receiver_id
    postTxRequest.senderId = "sender_bar"
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(Sep31CustomerInfoNeededException::class.java, ex)
    assertEquals("sep31-sender", (ex as Sep31CustomerInfoNeededException).type)

    // ----- QUOTE_ID IS USED ⬇️ -----
    // not found quote_id
    val senderId = "d2bd1412-e2f6-4047-ad70-a1a2f133b25c"
    postTxRequest.senderId = senderId
    request = Sep12GetCustomerRequest.builder().id(senderId).type("sep31-sender").build()
    every { customerIntegration.getCustomer(request) } returns Sep12GetCustomerResponse()

    postTxRequest.quoteId = "not-found-quote-id"
    every { quoteStore.findByQuoteId(any()) } returns null
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("quote(id=not-found-quote-id) was not found.", ex.message)

    // quote and tx amounts don't match
    val quoteId = "de762cda-a193-4961-861e-57b31fed6eb3"
    val quote = PojoSep38Quote()
    quote.sellAmount = "100.1"
    postTxRequest.amount = "100"
    postTxRequest.quoteId = quoteId
    every { quoteStore.findByQuoteId(quoteId) } returns quote

    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals(
      "Quote sell amount [100.1] is different from the SEP-31 transaction amount [100]",
      ex.message,
    )

    // quote and tx assets don't match (quote.sell_asset is null)
    quote.sellAmount = "100.00000"
    every { quoteStore.findByQuoteId(quoteId) } returns quote
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals(
      "Quote sell asset [null] is different from the SEP-31 transaction asset [stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP]",
      ex.message,
    )

    // quote and tx assets don't match
    quote.sellAsset = "stellar:USDC:zzz"
    every { quoteStore.findByQuoteId(quoteId) } returns quote
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals(
      "Quote sell asset [stellar:USDC:zzz] is different from the SEP-31 transaction asset [stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP]",
      ex.message,
    )

    // quote is missing the `fee` field
    quote.sellAsset = stellarUSDC
    every { quoteStore.findByQuoteId(quoteId) } returns quote
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(SepValidationException::class.java, ex)
    assertEquals("Quote is missing the 'fee' field", ex.message)
  }

  @Test
  fun test_postTransaction_withQuote() {
    val tomorrow = Instant.now().plus(1, ChronoUnit.DAYS)
    quote.expiresAt = tomorrow
    quote.id = "my_quote_id"
    quote.sellAsset = stellarUSDC
    quote.sellAmount = "100"
    quote.buyAsset = stellarJPYC
    quote.buyAmount = "12500"
    quote.totalPrice = "0.008"
    quote.price = "0.0072"
    quote.expiresAt = Instant.now()
    quote.fee = RateFee("10", stellarUSDC)
    every { quoteStore.findByQuoteId("my_quote_id") } returns quote

    val senderId = "d2bd1412-e2f6-4047-ad70-a1a2f133b25c"
    val receiverId = "137938d4-43a7-4252-a452-842adcee474c"
    val postTxRequest = Sep31PostTransactionRequest()
    postTxRequest.amount = "100"
    postTxRequest.assetCode = "USDC"
    postTxRequest.assetIssuer = "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
    postTxRequest.senderId = senderId
    postTxRequest.receiverId = receiverId
    postTxRequest.quoteId = "my_quote_id"
    postTxRequest.fields =
      Sep31TxnFields(
        hashMapOf(
          "receiver_account_number" to "1",
          "type" to "1",
          "receiver_routing_number" to "SWIFT",
        ),
      )

    // Make sure we can get the sender and receiver customers
    every { customerIntegration.getCustomer(any()) } returns Sep12GetCustomerResponse()

    // mock sep31 deposit info generation
    val txForDepositInfoGenerator = slot<Sep31Transaction>()
    every {
      sep31DepositInfoGenerator.getSep31DepositInfo(capture(txForDepositInfoGenerator))
    } answers
      {
        val tx: Sep31Transaction = txForDepositInfoGenerator.captured
        var memo = StringUtils.truncate(tx.id, 32)
        memo = StringUtils.leftPad(memo, 32, '0')
        memo = String(Base64.getEncoder().encode(memo.toByteArray()))
        Sep31DepositInfo(tx.stellarAccountId, memo, "hash")
      }

    // mock eventService
    val txEventSlot = slot<TransactionEvent>()
    every { eventPublishService.publish(capture(txEventSlot)) } just Runs

    // mock transaction save
    val slotTxn = slot<Sep31Transaction>()
    every { txnStore.save(capture(slotTxn)) } returns null

    // POST transaction
    val jwtToken = TestHelper.createJwtToken()
    var gotResponse: Sep31PostTransactionResponse? = null
    assertDoesNotThrow { gotResponse = sep31Service.postTransaction(jwtToken, postTxRequest) }

    // verify if the mocks were called
    var request = Sep12GetCustomerRequest.builder().id(senderId).type("sep31-sender").build()
    verify(exactly = 1) { customerIntegration.getCustomer(request) }
    request = Sep12GetCustomerRequest.builder().id(receiverId).type("sep31-receiver").build()
    verify(exactly = 1) { customerIntegration.getCustomer(request) }
    verify(exactly = 1) { quoteStore.findByQuoteId("my_quote_id") }
    verify(exactly = 1) { sep31DepositInfoGenerator.getSep31DepositInfo(any()) }
    verify(exactly = 1) { eventPublishService.publish(any()) }

    // validate the values of the saved sep31Transaction
    val gotTx = gson.toJson(slotTxn.captured)
    val txId = slotTxn.captured.id
    var memo = StringUtils.truncate(txId, 32)
    memo = StringUtils.leftPad(memo, 32, '0')
    memo = String(Base64.getEncoder().encode(memo.toByteArray()))
    val txStartedAt = slotTxn.captured.startedAt
    val wantTx =
      """{
      "id": "$txId",
      "status": "pending_sender",
      "amountFee": "10",
      "amountFeeAsset": "$stellarUSDC",
      "startedAt": "$txStartedAt",
      "quoteId": "my_quote_id",
      "clientDomain": "vibrant.stellar.org",
      "fields": {
        "receiver_account_number": "1",
        "type": "1",
        "receiver_routing_number": "SWIFT"
      },
      "amountIn": "100",
      "amountInAsset": "$stellarUSDC",
      "amountOut": "12500",
      "amountOutAsset": "$stellarJPYC",
      "stellarAccountId": "GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I",
      "stellarMemo": "$memo",
      "stellarMemoType": "hash",
      "stellarTransactions": [],
      "receiverId":"137938d4-43a7-4252-a452-842adcee474c",
      "senderId":"d2bd1412-e2f6-4047-ad70-a1a2f133b25c"
    }""".trimMargin()
    JSONAssert.assertEquals(wantTx, gotTx, true)

    // validate event response
    val wantEvent: TransactionEvent =
      TransactionEvent.builder()
        .eventId(txEventSlot.captured.eventId)
        .type(TransactionEvent.Type.TRANSACTION_CREATED)
        .id(txId)
        .sep(TransactionEvent.Sep.SEP_31)
        .kind(TransactionEvent.Kind.RECEIVE)
        .status(TransactionEvent.Status.PENDING_SENDER)
        .statusChange(TransactionEvent.StatusChange(null, TransactionEvent.Status.PENDING_SENDER))
        .amountExpected(Amount("100", stellarUSDC))
        .amountIn(Amount("100", stellarUSDC))
        .amountOut(Amount("12500", stellarJPYC))
        .amountFee(Amount("10", stellarUSDC))
        .quoteId("my_quote_id")
        .startedAt(txStartedAt)
        .updatedAt(txStartedAt)
        .completedAt(null)
        .transferReceivedAt(null)
        .message(null)
        .refunds(null)
        .stellarTransactions(null)
        .externalTransactionId(null)
        .custodialTransactionId(null)
        .sourceAccount(senderId)
        .destinationAccount(receiverId)
        .creator(
          StellarId.builder()
            .account("GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I")
            .memo(memo)
            .memoType("hash")
            .build(),
        )
        .build()
    assertEquals(wantEvent, txEventSlot.captured)

    // validate the final response
    val wantResponse =
      Sep31PostTransactionResponse.builder()
        .id(txId)
        .stellarAccountId("GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I")
        .stellarMemo(memo)
        .stellarMemoType("hash")
        .build()
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun test_postTransaction_withoutQuote_quoteRequired() {
    Sep31Service.Context.get().setAsset(asset)
    val senderId = "d2bd1412-e2f6-4047-ad70-a1a2f133b25c"
    val receiverId = "137938d4-43a7-4252-a452-842adcee474c"
    val postTxRequest = Sep31PostTransactionRequest()
    postTxRequest.amount = "100"
    postTxRequest.assetCode = "USDC"
    postTxRequest.assetIssuer = "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
    postTxRequest.senderId = senderId
    postTxRequest.receiverId = receiverId
    postTxRequest.fields =
      Sep31TxnFields(
        hashMapOf(
          "receiver_account_number" to "1",
          "type" to "1",
          "receiver_routing_number" to "SWIFT",
        ),
      )

    // Make sure we can get the sender and receiver customers
    every { customerIntegration.getCustomer(any()) } returns Sep12GetCustomerResponse()

    // POST transaction
    val jwtToken = TestHelper.createJwtToken()
    val ex: AnchorException = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("quotes_required is set to true; quote id cannot be empty", ex.message)

    // verify if the mocks were called
    var request = Sep12GetCustomerRequest.builder().id(senderId).type("sep31-sender").build()
    verify(exactly = 1) { customerIntegration.getCustomer(request) }
    request = Sep12GetCustomerRequest.builder().id(receiverId).type("sep31-receiver").build()
    verify(exactly = 1) { customerIntegration.getCustomer(request) }
  }

  @Test
  fun test_postTransaction_quoteNotSupported() {
    val assetServiceQuotesNotSupported: AssetService =
      ResourceJsonAssetService(
        "test_assets.json.quotes_not_supported",
      )
    sep31Service =
      Sep31Service(
        appConfig,
        sep31Config,
        txnStore,
        sep31DepositInfoGenerator,
        quoteStore,
        assetServiceQuotesNotSupported,
        feeIntegration,
        customerIntegration,
        eventPublishService,
      )

    val senderId = "d2bd1412-e2f6-4047-ad70-a1a2f133b25c"
    val receiverId = "137938d4-43a7-4252-a452-842adcee474c"
    val postTxRequest = Sep31PostTransactionRequest()
    postTxRequest.amount = "100"
    postTxRequest.assetCode = "USDC"
    postTxRequest.assetIssuer = "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5"
    postTxRequest.senderId = senderId
    postTxRequest.receiverId = receiverId
    postTxRequest.fields =
      Sep31TxnFields(
        hashMapOf(
          "receiver_account_number" to "1",
          "type" to "1",
          "receiver_routing_number" to "SWIFT",
        ),
      )

    // Provide fee response.
    every { feeIntegration.getFee(any()) } returns
      GetFeeResponse(
        Amount(
          "2",
          "stellar:USDC",
        ),
      )

    // POST transaction
    val jwtToken = TestHelper.createJwtToken()
    var gotResponse: Sep31PostTransactionResponse? = null
    assertDoesNotThrow { gotResponse = sep31Service.postTransaction(jwtToken, postTxRequest) }

    val wantResponse =
      Sep31PostTransactionResponse.builder()
        .id(gotResponse!!.id)
        .stellarAccountId("GA7FYRB5VREZKOBIIKHG5AVTPFGWUBPOBF7LTYG4GTMFVIOOD2DWAL7I")
        .stellarMemo("")
        .stellarMemoType("")
        .build()
    assertEquals(wantResponse, gotResponse)
  }

  @Test
  fun test_info_response() {
    val info = sep31Service.info
    val jpyc = info.receive.get("JPYC")!!
    val usdc = info.receive.get("USDC")!!

    // Test correctness of reading test_assets.json
    assertTrue(jpyc.enabled)
    assertTrue(jpyc.quotesSupported)
    assertTrue(jpyc.quotesRequired)
    assertEquals(0, jpyc.feeFixed)
    assertEquals(0, jpyc.feePercent)
    assertEquals(1, jpyc.minAmount)
    assertEquals(1000000, jpyc.maxAmount)
    assertNotNull(jpyc.sep12)
    assertNotNull(jpyc.sep12.sender)
    assertNotNull(jpyc.sep12.sender.types)
    assertEquals(1, jpyc.sep12.sender.types.size)
    assertNotNull(jpyc.sep12.receiver)
    assertNotNull(jpyc.sep12.receiver.types)
    assertEquals(1, jpyc.sep12.receiver.types.size)
    assertNotNull(jpyc.fields.transaction)
    assertEquals(3, jpyc.fields.transaction.size)

    assertTrue(usdc.enabled)
    assertTrue(usdc.quotesSupported)
    assertTrue(usdc.quotesRequired)
    assertEquals(0, usdc.feeFixed)
    assertEquals(0, usdc.feePercent)
    assertEquals(1, usdc.minAmount)
    assertEquals(1000000, usdc.maxAmount)
    assertNotNull(usdc.sep12)
    assertNotNull(usdc.sep12.sender)
    assertNotNull(usdc.sep12.sender.types)
    assertEquals(3, usdc.sep12.sender.types.size)
    assertNotNull(usdc.sep12.receiver)
    assertNotNull(usdc.sep12.receiver.types)
    assertEquals(2, usdc.sep12.receiver.types.size)
    assertNotNull(usdc.fields.transaction)
    assertEquals(3, usdc.fields.transaction.size)


    print(info)
  }
}
