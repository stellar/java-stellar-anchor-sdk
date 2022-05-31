package org.stellar.anchor.sep31

import com.google.gson.Gson
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.stellar.anchor.Constants
import org.stellar.anchor.TestHelper
import org.stellar.anchor.api.callback.CustomerIntegration
import org.stellar.anchor.api.callback.FeeIntegration
import org.stellar.anchor.api.exception.AnchorException
import org.stellar.anchor.api.exception.BadRequestException
import org.stellar.anchor.api.exception.SepValidationException
import org.stellar.anchor.api.sep.AssetInfo
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerRequest
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerResponse
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest.Sep31TxnFields
import org.stellar.anchor.api.shared.Amount
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.ResourceJsonAssetService
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.config.Sep31Config
import org.stellar.anchor.config.Sep31Config.PaymentType.STRICT_RECEIVE
import org.stellar.anchor.config.Sep31Config.PaymentType.STRICT_SEND
import org.stellar.anchor.event.EventPublishService
import org.stellar.anchor.sep10.JwtService
import org.stellar.anchor.sep31.Sep31Service.Sep31CustomerInfoNeededException
import org.stellar.anchor.sep31.Sep31Service.Sep31MissingFieldException
import org.stellar.anchor.sep38.PojoSep38Quote
import org.stellar.anchor.sep38.Sep38QuoteStore
import org.stellar.anchor.util.GsonUtils

internal class Sep31ServiceTest {
  companion object {
    val gson: Gson = GsonUtils.getInstance()

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
        "id": "quote_id",
        "expires_at": "2022-04-18T23:33:24.629719Z",
        "price": "5.0",
        "sell_asset": "USD",
        "sell_amount": "100",
        "sell_delivery_method": "SWIFT",
        "buy_asset": "USD",
        "buy_amount": "100",
        "buy_delivery_method": "SWIFT",
        "created_at": "2022-04-18T23:33:24.629719Z",
        "creator_account_id": "1234",
        "creator_memo": "5678",
        "creator_memo_type": "string",
        "transaction_id": "abcd"
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
        eventPublishService
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
  fun testUpdateAmountsNoQuote() {
    Sep31Service.Context.get().setTransaction(txn)
    Sep31Service.Context.get().setRequest(request)
    Sep31Service.Context.get().setFee(fee)
    every { sep31Config.paymentType } returns STRICT_SEND

    request.amount = "100"
    fee.amount = "2"
    sep31Service.updateAmounts()
    assertEquals(txn.amountIn, "100.00")
    assertEquals(txn.amountFee, "2.00")
    assertEquals(txn.amountOut, "98.00")

    every { sep31Config.paymentType } returns STRICT_RECEIVE
    sep31Service.updateAmounts()
    assertEquals("102.00", txn.amountIn)
    assertEquals("2.00", txn.amountFee)
    assertEquals("100.00", txn.amountOut)
  }

  @Test
  fun testUpdateAmounts2WithQuote() {
    Sep31Service.Context.get().setTransaction(txn)
    Sep31Service.Context.get().setRequest(request)
    Sep31Service.Context.get().setFee(fee)
    every { quoteStore.findByQuoteId(any()) } returns quote
    request.quoteId = "quote_id"

    // Fee is as sell asset
    every { sep31Config.paymentType } returns STRICT_SEND
    request.amount = "100"
    request.assetCode = "USDC"
    fee.amount = "2"
    fee.asset = "USDC"
    quote.sellAmount = "98"
    quote.sellAsset = "USDC"
    quote.buyAmount = "490"
    quote.buyAsset = "BRL"
    sep31Service.updateAmounts2()
    assertEquals("100.00", txn.amountIn)
    assertEquals(
      "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      txn.amountInAsset
    )
    assertEquals("2.00", txn.amountFee)
    assertEquals("USDC", txn.amountFeeAsset)
    assertEquals("490.00", txn.amountOut)
    assertEquals("BRL", txn.amountOutAsset)

    every { sep31Config.paymentType } returns STRICT_RECEIVE
    request.amount = "100"
    request.assetCode = "USDC"
    fee.amount = "2"
    fee.asset = "USDC"
    quote.sellAmount = "100"
    quote.sellAsset = "USDC"
    quote.buyAmount = "500"
    quote.buyAsset = "BRL"
    sep31Service.updateAmounts2()
    assertEquals("102.00", txn.amountIn)
    assertEquals(
      "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      txn.amountInAsset
    )
    assertEquals("2.00", txn.amountFee)
    assertEquals("USDC", txn.amountFeeAsset)
    assertEquals("500.00", txn.amountOut)
    assertEquals("BRL", txn.amountOutAsset)

    // Fee is as buy asset
    every { sep31Config.paymentType } returns STRICT_SEND
    request.amount = "100"
    request.assetCode = "USDC"
    fee.amount = "10"
    fee.asset = "BRL"
    quote.sellAmount = "100"
    quote.sellAsset = "USDC"
    quote.buyAmount = "500"
    quote.buyAsset = "BRL"
    sep31Service.updateAmounts2()

    assertEquals("100.00", txn.amountIn)
    assertEquals(
      "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      txn.amountInAsset
    )
    assertEquals("10.00", txn.amountFee)
    assertEquals("BRL", txn.amountFeeAsset)
    assertEquals("490.00", txn.amountOut)
    assertEquals("BRL", txn.amountOutAsset)

    every { sep31Config.paymentType } returns STRICT_RECEIVE
    request.amount = "100"
    request.assetCode = "USDC"
    fee.amount = "10"
    fee.asset = "BRL"
    quote.sellAmount = "102"
    quote.sellAsset = "USDC"
    quote.buyAmount = "510"
    quote.buyAsset = "BRL"
    sep31Service.updateAmounts2()
    assertEquals("102.00", txn.amountIn)
    assertEquals(
      "stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
      txn.amountInAsset
    )
    assertEquals("10.00", txn.amountFee)
    assertEquals("BRL", txn.amountFeeAsset)
    assertEquals("500.00", txn.amountOut)
    assertEquals("BRL", txn.amountOutAsset)
  }

  @Test
  fun testUpdateAmountsWithQuote() {
    Sep31Service.Context.get().setTransaction(txn)
    Sep31Service.Context.get().setRequest(request)
    Sep31Service.Context.get().setFee(fee)
    every { quoteStore.findByQuoteId(any()) } returns quote
    request.quoteId = "quote_id"

    // Fee is as sell asset
    every { sep31Config.paymentType } throws Exception("paymentType must not be called")
    request.amount = "100"
    request.assetCode = "USDC"
    quote.sellAmount = "100"
    quote.sellAsset = "USDC"
    // TODO: Add fee information.
    quote.buyAmount = "490"
    quote.buyAsset = "BRL"
    sep31Service.updateAmounts()
    // TODO: Add fee validation.
    assertEquals("100.00", txn.amountIn)
    assertEquals("USDC", txn.amountInAsset)
    assertEquals("490.00", txn.amountOut)
    assertEquals("BRL", txn.amountOutAsset)

    request.amount = "102"
    request.assetCode = "USDC"
    quote.sellAmount = "102"
    quote.sellAsset = "USDC"
    // TODO: Add fee information.
    quote.buyAmount = "500"
    quote.buyAsset = "BRL"
    sep31Service.updateAmounts()
    // TODO: Add fee validation.
    assertEquals("102.00", txn.amountIn)
    assertEquals("USDC", txn.amountInAsset)
    assertEquals("500.00", txn.amountOut)
    assertEquals("BRL", txn.amountOutAsset)
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

    // unsupported language
    postTxRequest.amount = "100"
    postTxRequest.lang = "es"
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(SepValidationException::class.java, ex)
    assertEquals("unsupported language: es", ex.message)

    // missing required fields
    postTxRequest.lang = null
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
      "missing field names don't match"
    )
    assertTrue(
      gotMissingFieldsNames.containsAll(wantMissingFieldsNames),
      "missing field names don't match"
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
    var request = Sep12GetCustomerRequest.builder().id(receiverId).build()
    every { customerIntegration.getCustomer(request) } returns Sep12GetCustomerResponse()
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals("sender_id cannot be empty.", ex.message)

    // not found receiver_id
    postTxRequest.senderId = "sender_bar"
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(Sep31CustomerInfoNeededException::class.java, ex)
    assertEquals("sep31-sender", (ex as Sep31CustomerInfoNeededException).type)

    // not found quote_id
    val senderId = "d2bd1412-e2f6-4047-ad70-a1a2f133b25c"
    postTxRequest.senderId = senderId
    request = Sep12GetCustomerRequest.builder().id(senderId).build()
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
      ex.message
    )

    // quote and tx assets don't match (quote.sell_asset is null)
    quote.sellAmount = "100.00000"
    every { quoteStore.findByQuoteId(quoteId) } returns quote
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals(
      "Quote sell asset [null] is different from the SEP-31 transaction asset [stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5]",
      ex.message
    )

    // quote and tx assets don't match
    quote.sellAsset = "stellar:USDC:zzz"
    every { quoteStore.findByQuoteId(quoteId) } returns quote
    ex = assertThrows { sep31Service.postTransaction(jwtToken, postTxRequest) }
    assertInstanceOf(BadRequestException::class.java, ex)
    assertEquals(
      "Quote sell asset [stellar:USDC:zzz] is different from the SEP-31 transaction asset [stellar:USDC:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5]",
      ex.message
    )
  }
}
