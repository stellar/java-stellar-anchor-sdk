package org.stellar.anchor.sep31

import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.Constants
import org.stellar.anchor.asset.AssetInfo
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.ResourceJsonAssetService
import org.stellar.anchor.config.AppConfig
import org.stellar.anchor.config.Sep31Config
import org.stellar.anchor.config.Sep31Config.PaymentType.STRICT_RECEIVE
import org.stellar.anchor.config.Sep31Config.PaymentType.STRICT_SEND
import org.stellar.anchor.dto.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.event.EventService
import org.stellar.anchor.integration.customer.CustomerIntegration
import org.stellar.anchor.integration.fee.FeeIntegration
import org.stellar.anchor.sep10.JwtService
import org.stellar.anchor.sep38.PojoSep38Quote
import org.stellar.anchor.sep38.Sep38QuoteStore
import org.stellar.anchor.util.GsonUtils
import org.stellar.platform.apis.shared.Amount

internal class Sep31ServiceTest {
  companion object {
    val gson: Gson = GsonUtils.getInstance()
  }

  private val assetService: AssetService = ResourceJsonAssetService("test_assets.json")

  @MockK(relaxed = true) private lateinit var txnStore: Sep31TransactionStore

  @MockK(relaxed = true) lateinit var appConfig: AppConfig
  @MockK(relaxed = true) lateinit var sep31Config: Sep31Config
  @MockK(relaxed = true) lateinit var quoteStore: Sep38QuoteStore
  @MockK(relaxed = true) lateinit var feeIntegration: FeeIntegration
  @MockK(relaxed = true) lateinit var customerIntegration: CustomerIntegration
  @MockK(relaxed = true) lateinit var eventService: EventService

  private lateinit var jwtService: JwtService
  private lateinit var sep31Service: Sep31Service

  lateinit var request: Sep31PostTransactionRequest
  lateinit var txn: PojoSep31Transaction
  lateinit var fee: Amount
  lateinit var asset: AssetInfo
  lateinit var quote: PojoSep38Quote

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { appConfig.stellarNetworkPassphrase } returns Constants.TEST_NETWORK_PASS_PHRASE
    every { appConfig.hostUrl } returns Constants.TEST_HOST_URL
    every { appConfig.jwtSecretKey } returns Constants.TEST_JWT_SECRET
    every { sep31Config.paymentType } returns STRICT_SEND
    every { txnStore.newTransaction() } returns PojoSep31Transaction()

    jwtService = spyk(JwtService(appConfig))

    sep31Service =
      Sep31Service(
        appConfig,
        sep31Config,
        txnStore,
        quoteStore,
        assetService,
        feeIntegration,
        customerIntegration,
        eventService
      )

    request = gson.fromJson(requestJson, Sep31PostTransactionRequest::class.java)
    txn = gson.fromJson(txnJson, PojoSep31Transaction::class.java)
    fee = gson.fromJson(feeJson, Amount::class.java)
    asset = gson.fromJson(assetJson, AssetInfo::class.java)
    quote = gson.fromJson(quoteJson, PojoSep38Quote::class.java)
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
  fun testUpdateAmountsWithQuote() {
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
    sep31Service.updateAmounts()
    assertEquals("100.00", txn.amountIn)
    assertEquals("USDC", txn.amountInAsset)
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
    sep31Service.updateAmounts()
    assertEquals("102.00", txn.amountIn)
    assertEquals("USDC", txn.amountInAsset)
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
    sep31Service.updateAmounts()

    assertEquals("100.00", txn.amountIn)
    assertEquals("USDC", txn.amountInAsset)
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
    sep31Service.updateAmounts()
    assertEquals("102.00", txn.amountIn)
    assertEquals("USDC", txn.amountInAsset)
    assertEquals("10.00", txn.amountFee)
    assertEquals("BRL", txn.amountFeeAsset)
    assertEquals("500.00", txn.amountOut)
    assertEquals("BRL", txn.amountOutAsset)
  }

  val requestJson =
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
  val feeJson = """
        {
          "amount": "2",
          "asset": "USDC"
        }
    """

  val assetJson =
    """
            {
              "code": "USDC",
              "issuer": "GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5",
              "distribution_account": "GDVARAZQD3B5QKKQG2AE455HXLD3NYFWRMPBGXSH2VE3L3CF23CAZDUB",
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

  val txnJson =
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

  val quoteJson =
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
