package org.stellar.anchor.platform

import java.time.temporal.ChronoUnit.SECONDS
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.JSONCompareMode.LENIENT
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.platform.PatchTransactionRequest
import org.stellar.anchor.api.platform.PatchTransactionsRequest
import org.stellar.anchor.api.sep.SepTransactionStatus
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.api.sep.sep12.Sep12Status
import org.stellar.anchor.api.sep.sep31.Sep31GetTransactionResponse
import org.stellar.anchor.api.sep.sep31.Sep31PostTransactionRequest
import org.stellar.anchor.platform.AnchorPlatformIntegrationTest.Companion.jwt
import org.stellar.anchor.platform.AnchorPlatformIntegrationTest.Companion.toml
import org.stellar.anchor.util.GsonUtils

lateinit var sep31Client: Sep31Client
lateinit var savedTxn: Sep31GetTransactionResponse

class Sep31Tests {
  companion object {
    fun setup() {
      println("Performing SEP31 tests...")
      if (!::sep31Client.isInitialized)
        sep31Client = Sep31Client(toml.getString("DIRECT_PAYMENT_SERVER"), jwt)
    }
    fun `test info endpoint`() {
      printRequest("Calling GET /info")
      val info = sep31Client.getInfo()
      JSONAssert.assertEquals(gson.toJson(info), expectedSep31Info, JSONCompareMode.STRICT)
    }

    fun `test post and get transactions`() {
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
      val txnRequest = gson.fromJson(postTxnRequest, Sep31PostTransactionRequest::class.java)
      txnRequest.senderId = senderCustomer!!.id
      txnRequest.receiverId = receiverCustomer!!.id
      txnRequest.quoteId = quote.id
      val postTxResponse = sep31Client.postTransaction(txnRequest)
      assertEquals(
        "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
        postTxResponse.stellarAccountId
      )

      // GET Sep31 transaction
      savedTxn = sep31Client.getTransaction(postTxResponse.id)
      JSONAssert.assertEquals(expectedTxn, json(savedTxn), LENIENT)
      assertEquals(postTxResponse.id, savedTxn.transaction.id)
      assertEquals(postTxResponse.stellarMemo, savedTxn.transaction.stellarMemo)
      assertEquals(SepTransactionStatus.PENDING_SENDER.status, savedTxn.transaction.status)
    }

    fun testBadAsset() {
      val customer =
        GsonUtils.getInstance().fromJson(testCustomer1Json, Sep12PutCustomerRequest::class.java)
      val pr = sep12Client.putCustomer(customer)

      // Post Sep31 transaction.
      val txnRequest = gson.fromJson(postTxnRequest, Sep31PostTransactionRequest::class.java)
      txnRequest.assetCode = "bad-asset-code"
      txnRequest.receiverId = pr!!.id
      assertThrows<SepException> { sep31Client.postTransaction(txnRequest) }
    }

    fun `test patch, get and compare`() {
      val patch = gson.fromJson(patchRequest, PatchTransactionsRequest::class.java)
      // create patch request and patch
      patch.records[0].id = savedTxn.transaction.id
      platformApiClient.patchTransaction(patch)

      // check if the patched transactions are as expected
      var afterPatch = platformApiClient.getTransaction(savedTxn.transaction.id)
      assertEquals(afterPatch.id, savedTxn.transaction.id)
      JSONAssert.assertEquals(expectedAfterPatch, json(afterPatch), LENIENT)

      // Test patch idempotency
      afterPatch = platformApiClient.getTransaction(savedTxn.transaction.id)
      assertEquals(afterPatch.id, savedTxn.transaction.id)
      JSONAssert.assertEquals(expectedAfterPatch, json(afterPatch), LENIENT)
    }

    fun `test bad requests`() {
      // Create sender customer
      val senderCustomerRequest =
        GsonUtils.getInstance().fromJson(testCustomer1Json, Sep12PutCustomerRequest::class.java)
      val senderCustomer = sep12Client.putCustomer(senderCustomerRequest, TYPE_MULTIPART_FORM_DATA)

      // Create receiver customer
      val receiverCustomerRequest =
        GsonUtils.getInstance().fromJson(testCustomer2Json, Sep12PutCustomerRequest::class.java)
      val receiverCustomer = sep12Client.putCustomer(receiverCustomerRequest)
      val quote =
        sep38Client.postQuote(
          "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
          "10",
          "stellar:JPYC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
        )

      // POST SEP-31 transaction
      val txnRequest = gson.fromJson(postTxnRequest, Sep31PostTransactionRequest::class.java)
      txnRequest.senderId = senderCustomer!!.id
      txnRequest.receiverId = receiverCustomer!!.id
      txnRequest.quoteId = quote.id
      val postTxResponse = sep31Client.postTransaction(txnRequest)

      // GET platformAPI transaction
      val getTxResponse = platformApiClient.getTransaction(postTxResponse.id)
      assertEquals(postTxResponse.id, getTxResponse.id)
      assertEquals(SepTransactionStatus.PENDING_SENDER, getTxResponse.status)
      assertEquals(txnRequest.amount, getTxResponse.amountIn.amount)
      assertTrue(getTxResponse.amountIn.asset.contains(txnRequest.assetCode))
      assertEquals(31, getTxResponse.sep)
      assertNull(getTxResponse.completedAt)
      assertNotNull(getTxResponse.startedAt)
      assertTrue(getTxResponse.updatedAt >= getTxResponse.startedAt)

      // Modify the customer by erasing its clabe_number to simulate an invalid clabe_number
      sep12Client.invalidateCustomerClabe(receiverCustomer.id)
      var updatedReceiverCustomer = sep12Client.getCustomer(receiverCustomer.id, "sep31-receiver")
      assertEquals(Sep12Status.NEEDS_INFO, updatedReceiverCustomer?.status)
      assertNotNull(updatedReceiverCustomer?.fields?.get("clabe_number"))
      assertNull(updatedReceiverCustomer?.providedFields?.get("clabe_number"))

      // PATCH {platformAPI}/transaction status to PENDING_CUSTOMER_INFO_UPDATE, since the
      // clabe_number
      // was invalidated.
      var patchTxRequest =
        PatchTransactionRequest.builder()
          .id(getTxResponse.id)
          .status(SepTransactionStatus.PENDING_CUSTOMER_INFO_UPDATE.name)
          .message("The receiving customer clabe_number is invalid!")
          .build()
      var patchTxResponse =
        platformApiClient.patchTransaction(
          PatchTransactionsRequest.builder().records(listOf(patchTxRequest)).build()
        )
      assertEquals(1, patchTxResponse.records.size)
      var patchedTx = patchTxResponse.records[0]
      assertEquals(getTxResponse.id, patchedTx.id)
      assertEquals(SepTransactionStatus.PENDING_CUSTOMER_INFO_UPDATE.name, patchedTx.status)
      assertEquals(31, patchedTx.sep)
      assertEquals("The receiving customer clabe_number is invalid!", patchedTx.message)
      assertTrue(patchedTx.updatedAt > patchedTx.startedAt)

      // GET SEP-31 transaction should return PENDING_CUSTOMER_INFO_UPDATE with a message
      var gotSep31TxResponse = sep31Client.getTransaction(postTxResponse.id)
      assertEquals(postTxResponse.id, gotSep31TxResponse.transaction.id)
      assertEquals(
        SepTransactionStatus.PENDING_CUSTOMER_INFO_UPDATE.name,
        gotSep31TxResponse.transaction.status
      )
      assertEquals(
        "The receiving customer clabe_number is invalid!",
        gotSep31TxResponse.transaction.requiredInfoMessage
      )
      assertNull(gotSep31TxResponse.transaction.completedAt)

      // PUT sep12/customer with the correct clabe_number
      sep12Client.putCustomer(
        Sep12PutCustomerRequest.builder().id(receiverCustomer.id).clabeNumber("5678").build()
      )
      updatedReceiverCustomer = sep12Client.getCustomer(receiverCustomer.id, "sep31-receiver")
      assertEquals(Sep12Status.ACCEPTED, updatedReceiverCustomer?.status)
      assertNull(updatedReceiverCustomer?.fields?.get("clabe_number"))
      assertNotNull(updatedReceiverCustomer?.providedFields?.get("clabe_number"))

      // PATCH {platformAPI}/transaction status to COMPLETED, since the clabe_number was updated
      // correctly.
      patchTxRequest =
        PatchTransactionRequest.builder()
          .id(getTxResponse.id)
          .status(SepTransactionStatus.COMPLETED.name)
          .build()
      patchTxResponse =
        platformApiClient.patchTransaction(
          PatchTransactionsRequest.builder().records(listOf(patchTxRequest)).build()
        )
      assertEquals(1, patchTxResponse.records.size)
      patchedTx = patchTxResponse.records[0]
      assertEquals(getTxResponse.id, patchedTx.id)
      assertEquals(SepTransactionStatus.COMPLETED.name, patchedTx.status)
      assertEquals(31, patchedTx.sep)
      assertNull(patchedTx.message)
      assertTrue(patchedTx.startedAt < patchedTx.updatedAt)
      assertEquals(patchedTx.updatedAt, patchedTx.completedAt)

      // GET SEP-31 transaction should return COMPLETED with no message
      gotSep31TxResponse = sep31Client.getTransaction(postTxResponse.id)
      assertEquals(postTxResponse.id, gotSep31TxResponse.transaction.id)
      assertEquals(SepTransactionStatus.COMPLETED.name, gotSep31TxResponse.transaction.status)
      assertNull(gotSep31TxResponse.transaction.requiredInfoMessage)
      assertEquals(
        patchedTx.completedAt.truncatedTo(SECONDS),
        gotSep31TxResponse.transaction.completedAt.truncatedTo(SECONDS)
      )
    }
  }
}

fun sep31TestAll() {
  Sep31Tests.setup()

  println("Performing Sep31 tests...")
  Sep31Tests.`test info endpoint`()
  Sep31Tests.`test post and get transactions`()
  Sep31Tests.`test patch, get and compare`()
  Sep31Tests.testBadAsset()
}

private const val postTxnRequest =
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

private const val expectedTxn =
  """
  {
  "transaction": {
    "status": "pending_sender",
    "amount_in": "10",
    "amount_in_asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
    "amount_out": "1071.4285982",
    "amount_out_asset": "stellar:JPYC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
    "amount_fee": "1.00",
    "amount_fee_asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
    "stellar_account_id": "GBN4NNCDGJO4XW4KQU3CBIESUJWFVBUZPOKUZHT7W7WRB7CWOA7BXVQF",
    "stellar_memo_type": "hash"
  }
}
"""

private const val expectedSep31Info =
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

private const val patchRequest =
  """
{
  "records": [
    {
      "id": "",
      "status": "completed",
      "message": "this is the message",
      "refunds": {
        "amount_refunded": {
          "amount": "1",
          "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
        },
        "amount_fee": {
          "amount": "0.1",
          "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
        },
        "payments": [
          {
            "id": 1,
            "amount": {
              "amount": "0.6",
              "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
            },
            "fee": {
              "amount": "0.1",
              "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
            }
          },
          {
            "id": 2,
            "amount": {
              "amount": "0.4",
              "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
            },
            "fee": {
              "amount": "0",
              "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
            }
          }
        ]
      }
    }
  ]
}  
"""

private const val expectedAfterPatch =
  """
  {
  "sep": 31,
  "kind": "receive",
  "status": "completed",
  "amount_expected": {
    "amount": "10",
    "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
  },
  "amount_in": {
    "amount": "10",
    "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
  },
  "amount_out": {
    "amount": "1071.4285982",
    "asset": "stellar:JPYC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
  },
  "amount_fee": {
    "amount": "1.00",
    "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
  },
  "message": "this is the message",
  "refunds": {
    "amount_refunded": {
      "amount": "1",
      "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
    },
    "amount_fee": {
      "amount": "0.1",
      "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
    },
    "payments": [
      {
        "id": "1",
        "id_type": "stellar",
        "amount": {
          "amount": "0.6",
          "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
        },
        "fee": {
          "amount": "0.1",
          "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
        }
      },
      {
        "id": "2",
        "id_type": "stellar",
        "amount": {
          "amount": "0.4",
          "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
        },
        "fee": {
          "amount": "0",
          "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
        }
      }
    ]
  },
  "customers": {
    "sender": {
    },
    "receiver": {
    }
  },
  "creator": {
    "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
  }
}
"""
