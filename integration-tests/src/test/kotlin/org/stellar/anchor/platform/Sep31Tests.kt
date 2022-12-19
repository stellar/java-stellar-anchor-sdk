package org.stellar.anchor.platform

import kotlin.test.assertEquals
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.event.models.TransactionEvent
import org.stellar.anchor.platform.AnchorPlatformIntegrationTest.Companion.jwt
import org.stellar.anchor.platform.AnchorPlatformIntegrationTest.Companion.toml
import org.stellar.anchor.util.GsonUtils

lateinit var sep31Client: Sep31Client

const val postTxnJson =
  """{
    "amount": "10",
    "asset_code": "USDC",
    "asset_issuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
    "receiver_id": "MOCK_RECEIVER_ID",
    "sender_id": "MOCK_SENDER_ID",
    "fields": {
        "transaction": {
            "receiver_routing_number": "r0123",
            "receiver_account_number": "a0456",
            "type": "SWIFT"
        }
    }
}"""

class Sep31Tests {
  companion object {
    fun setup() {
      println("Performing SEP31 tests...")
      if (!::sep31Client.isInitialized)
        sep31Client = Sep31Client(toml.getString("DIRECT_PAYMENT_SERVER"), jwt)
    }
    fun `test Sep31 info endpoint`() {
      printRequest("Calling GET /info")
      val info = sep31Client.getInfo()
      JSONAssert.assertEquals(gson.toJson(info), wantedSep31Info, JSONCompareMode.STRICT)
    }

    fun testSep31PostAndGetTransaction() {
      // Create sender customer
      val senderCustomerRequest =
        GsonUtils.getInstance().fromJson(testCustomer1Json, Sep12PutCustomerRequest::class.java)
      val senderCustomer = sep12Client.putCustomer(senderCustomerRequest)

      // Create receiver customer
      val receiverCustomerRequest =
        GsonUtils.getInstance().fromJson(testCustomer2Json, Sep12PutCustomerRequest::class.java)
      val receiverCustomer = sep12Client.putCustomer(receiverCustomerRequest)

      // Create asset quote
      val quote =
        sep38Client.postQuote(
          "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "10",
          "stellar:JPYC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
        )

      // POST Sep31 transaction
      val txnRequest = gson.fromJson(postTxnJson, Sep31PostTransactionRequest::class.java)
      txnRequest.senderId = senderCustomer!!.id
      txnRequest.receiverId = receiverCustomer!!.id
      txnRequest.quoteId = quote.id
      val postTxResponse = sep31Client.postTransaction(txnRequest)
      assertEquals(
        "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
        postTxResponse.stellarAccountId
      )

      // GET Sep31 transaction
      val getTxResponse = sep31Client.getTransaction(postTxResponse.id)
      assertEquals(postTxResponse.id, getTxResponse.transaction.id)
      assertEquals(postTxResponse.stellarAccountId, getTxResponse.transaction.stellarAccountId)
      assertEquals(postTxResponse.stellarMemo, getTxResponse.transaction.stellarMemo)
      assertEquals(postTxResponse.stellarMemoType, getTxResponse.transaction.stellarMemoType)
      assertEquals(TransactionEvent.Status.PENDING_SENDER.status, getTxResponse.transaction.status)
    }

    fun testBadAsset() {
      val customer =
        GsonUtils.getInstance().fromJson(testCustomer1Json, Sep12PutCustomerRequest::class.java)
      val pr = sep12Client.putCustomer(customer)

      // Post Sep31 transaction.
      val txnRequest = gson.fromJson(postTxnJson, Sep31PostTransactionRequest::class.java)
      txnRequest.assetCode = "bad-asset-code"
      txnRequest.receiverId = pr!!.id
      assertThrows<SepException> { sep31Client.postTransaction(txnRequest) }
    }
  }
}

fun sep31TestAll() {
  Sep31Tests.setup()

  Sep31Tests.`test Sep31 info endpoint`()
  Sep31Tests.testSep31PostAndGetTransaction()
  Sep31Tests.testBadAsset()
}

val wantedSep31Info =
  """
  {
    "receive": {
      "JPYC": {
        "enabled": true,
        "quotes_supported": true,
        "quotes_required": false,
        "fee_fixed": 0,
        "fee_percent": 0,
        "min_amount": 1,
        "max_amount": 1000000,
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
                "description": "U.S. citizens receiving JPY"
              },
              "sep31-foreign-receiver": {
                "description": "non-U.S. citizens receiving JPY"
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
      "USDC": {
        "enabled": true,
        "quotes_supported": true,
        "quotes_required": false,
        "fee_fixed": 0,
        "fee_percent": 0,
        "min_amount": 1,
        "max_amount": 1000000,
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
      }
    }
  }
"""
    .trimIndent()
