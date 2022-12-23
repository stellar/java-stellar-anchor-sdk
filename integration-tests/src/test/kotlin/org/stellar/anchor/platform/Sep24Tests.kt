@file:Suppress("UNCHECKED_CAST")

package org.stellar.anchor.platform

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.JSONCompareMode.LENIENT
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.platform.PatchTransactionsRequest
import org.stellar.anchor.api.sep.sep24.GetTransactionResponse
import org.stellar.anchor.platform.AnchorPlatformIntegrationTest.Companion.jwt
import org.stellar.anchor.platform.AnchorPlatformIntegrationTest.Companion.toml

lateinit var sep24Client: Sep24Client

lateinit var savedWithdrawTransaction: GetTransactionResponse
lateinit var savedDepositTransaction: GetTransactionResponse

class Sep24Tests {
  companion object {
    fun setup() {
      sep24Client = Sep24Client(toml.getString("TRANSFER_SERVER_SEP0024"), jwt)
    }
    fun `test Sep24 info endpoint`() {
      printRequest("Calling GET /info")
      val info = sep24Client.getInfo()
      JSONAssert.assertEquals(gson.toJson(info), expectedSep24Info, JSONCompareMode.STRICT)
    }

    fun `test Sep24 withdraw`() {
      printRequest("POST /transactions/withdraw/interactive")
      val withdrawRequest = gson.fromJson(withdrawRequest, HashMap::class.java)
      val txn = sep24Client.withdraw(withdrawRequest as HashMap<String, String>)
      printResponse("POST /transactions/withdraw/interactive response:", txn)
      savedWithdrawTransaction = sep24Client.getTransaction(txn.id, "USDC")
      printResponse(savedWithdrawTransaction)
      JSONAssert.assertEquals(
        expectedSep24WithdrawResponse,
        json(savedWithdrawTransaction),
        LENIENT
      )
    }

    fun `test Sep24 deposit`() {
      printRequest("POST /transactions/withdraw/interactive")
      val depositRequest = gson.fromJson(depositRequest, HashMap::class.java)
      val txn = sep24Client.deposit(depositRequest as HashMap<String, String>)
      printResponse("POST /transactions/deposit/interactive response:", txn)
      savedDepositTransaction = sep24Client.getTransaction(txn.id, "USDC")
      printResponse(savedDepositTransaction)
      JSONAssert.assertEquals(expectedSep24DepositResponse, json(savedDepositTransaction), LENIENT)
    }

    fun `test PlatformAPI GET transaction for deposit and withdrawal`() {
      val actualWithdrawTxn =
        platformApiClient.getTransaction(savedWithdrawTransaction.transaction.id)
      assertEquals(actualWithdrawTxn.id, savedWithdrawTransaction.transaction.id)
      JSONAssert.assertEquals(expectedWithdrawTransactionResponse, json(actualWithdrawTxn), LENIENT)

      val actualDepositTxn =
        platformApiClient.getTransaction(savedDepositTransaction.transaction.id)
      printResponse(actualDepositTxn)
      assertEquals(actualDepositTxn.id, savedDepositTransaction.transaction.id)
      JSONAssert.assertEquals(expectedDepositTransactionResponse, json(actualDepositTxn), LENIENT)
    }

    fun `test patch, get and compare`() {
      val patchRequest =
        gson.fromJson(patchWithdrawTransactionRequest, PatchTransactionsRequest::class.java)
      // create patch request and patch
      patchRequest.records[0].id = savedWithdrawTransaction.transaction.id
      patchRequest.records[1].id = savedDepositTransaction.transaction.id
      platformApiClient.patchTransaction(patchRequest)

      // check if the patched transactions are as expected
      var afterPatchWithdraw =
        platformApiClient.getTransaction(savedWithdrawTransaction.transaction.id)
      assertEquals(afterPatchWithdraw.id, savedWithdrawTransaction.transaction.id)
      JSONAssert.assertEquals(expectedAfterPatchWithdraw, json(afterPatchWithdraw), LENIENT)

      var afterPatchDeposit =
        platformApiClient.getTransaction(savedDepositTransaction.transaction.id)
      assertEquals(afterPatchDeposit.id, savedDepositTransaction.transaction.id)
      JSONAssert.assertEquals(expectedAfterPatchDeposit, json(afterPatchDeposit), LENIENT)

      // Test patch idempotency
      afterPatchWithdraw = platformApiClient.getTransaction(savedWithdrawTransaction.transaction.id)
      assertEquals(afterPatchWithdraw.id, savedWithdrawTransaction.transaction.id)
      JSONAssert.assertEquals(expectedAfterPatchWithdraw, json(afterPatchWithdraw), LENIENT)

      afterPatchDeposit = platformApiClient.getTransaction(savedDepositTransaction.transaction.id)
      assertEquals(afterPatchDeposit.id, savedDepositTransaction.transaction.id)
      JSONAssert.assertEquals(expectedAfterPatchDeposit, json(afterPatchDeposit), LENIENT)
    }

    fun `test GET transactions with bad ids`() {
      val badTxnIds = listOf("null", "bad id", "123", null)
      for (txnId in badTxnIds) {
        assertThrows<SepException> { platformApiClient.getTransaction(txnId) }
      }
    }
  }
}

fun sep24TestAll() {
  Sep24Tests.setup()

  println("Performing SEP24 tests...")
  Sep24Tests.`test Sep24 info endpoint`()
  Sep24Tests.`test Sep24 withdraw`()
  Sep24Tests.`test Sep24 deposit`()
  Sep24Tests.`test PlatformAPI GET transaction for deposit and withdrawal`()
  Sep24Tests.`test patch, get and compare`()
  Sep24Tests.`test GET transactions with bad ids`()
}

const val withdrawRequest =
  """{
    "asset_code": "USDC",
    "asset_issuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
    "account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
    "lang": "en"
}"""

const val depositRequest =
  """{
    "asset_code": "USDC",
    "asset_issuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
    "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
    "lang": "en"
}"""

val patchWithdrawTransactionRequest =
  """
{
  "records": [
    {
      "id": "",
      "status": "completed",
      "amount_in": {
        "amount": "10",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_out": {
        "amount": "10",
        "asset": "iso4217:USD"
      },
      "amount_fee": {
        "amount": "1",
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
    },
    {
      "id": "",
      "status": "completed",
      "amount_in": {
        "amount": "100",
        "asset": "iso4217:USD"
      },
      "amount_out": {
        "amount": "100",
        "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
      },
      "amount_fee": {
        "amount": "1",
        "asset": "iso4217:USD"
      },
      "message": "this is the message"
    }
  ]
}
"""
    .trimIndent()

val expectedAfterPatchWithdraw =
  """
{
  "sep": 24,
  "kind": "withdrawal",
  "status": "completed",
  "amount_in": {
    "amount": "10",
    "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
  },
  "amount_out": {
    "amount": "10",
    "asset": "iso4217:USD"
  },
  "amount_fee": {
    "amount": "1",
    "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
  },
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
  }
}"""
    .trimIndent()

val expectedAfterPatchDeposit =
  """
  {
    "sep": 24,
    "kind": "deposit",
    "status": "completed",
    "amount_in": {
      "amount": "100",
      "asset": "iso4217:USD"
    },
    "amount_out": {
      "amount": "100",
      "asset": "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
    },
    "amount_fee": {
      "amount": "1",
      "asset": "iso4217:USD"
    }
  }

"""
    .trimIndent()

val expectedSep24Info =
  """
  {
    "deposit": {
      "JPYC": {
        "enabled": true,
        "fee_fixed": 0,
        "fee_percent": 0,
        "min_amount": 1,
        "max_amount": 1000000,
        "fee_minimum": 0
      },
      "USD": {
        "enabled": true,
        "fee_fixed": 0,
        "fee_percent": 0,
        "min_amount": 0,
        "max_amount": 10000,
        "fee_minimum": 0
      },
      "USDC": {
        "enabled": true,
        "fee_fixed": 0,
        "fee_percent": 0,
        "min_amount": 1,
        "max_amount": 1000000,
        "fee_minimum": 0
      }
    },
    "withdraw": {
      "JPYC": {
        "enabled": true,
        "fee_fixed": 0,
        "fee_percent": 0,
        "min_amount": 1,
        "max_amount": 1000000
      },
      "USD": {
        "enabled": true,
        "fee_fixed": 0,
        "fee_percent": 0,
        "min_amount": 0,
        "max_amount": 10000
      },
      "USDC": {
        "enabled": true,
        "fee_fixed": 0,
        "fee_percent": 0,
        "min_amount": 1,
        "max_amount": 1000000
      }
    },
    "fee": {
      "enabled": true
    },
    "features": {
      "account_creation": true,
      "claimable_balances": true
    }
  }
"""
    .trimIndent()

val expectedSep24WithdrawResponse =
  """
  {
    "transaction": {
      "kind": "withdrawal",
      "status": "incomplete",
      "more_info_url": "http://www.stellar.org",
      "refunded": false,
      "from": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4"
    }
  }
"""
    .trimIndent()

val expectedSep24DepositResponse =
  """
  {
    "transaction": {
      "kind": "deposit",
      "status": "incomplete",
      "more_info_url": "http://www.stellar.org",
      "refunded": false,
      "to": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  }
"""
    .trimIndent()

val expectedWithdrawTransactionResponse =
  """
  {
    "sep": 24,
    "kind": "withdrawal",
    "status": "incomplete"
  }
"""
    .trimIndent()

val expectedDepositTransactionResponse =
  """
  {
    "sep": 24,
    "kind": "deposit",
    "status": "incomplete"
  }
"""
    .trimIndent()
