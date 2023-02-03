@file:Suppress("UNCHECKED_CAST")

package org.stellar.anchor.platform

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.LENIENT
import org.springframework.web.util.UriComponentsBuilder
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.platform.PatchTransactionsRequest
import org.stellar.anchor.api.sep.sep24.Sep24GetTransactionResponse
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.auth.Sep24InteractiveUrlJwt
import org.stellar.anchor.auth.Sep24MoreInfoUrlJwt
import org.stellar.anchor.platform.AnchorPlatformIntegrationTest.Companion.SEP10_JWT_SECRET
import org.stellar.anchor.platform.AnchorPlatformIntegrationTest.Companion.SEP24_INTERACTIVE_URL_JWT_SECRET
import org.stellar.anchor.platform.AnchorPlatformIntegrationTest.Companion.SEP24_MORE_INFO_URL_JWT_SECRET
import org.stellar.anchor.platform.AnchorPlatformIntegrationTest.Companion.jwt
import org.stellar.anchor.platform.AnchorPlatformIntegrationTest.Companion.toml

lateinit var sep24Client: Sep24Client
lateinit var savedWithdrawTxn: Sep24GetTransactionResponse
lateinit var savedDepositTxn: Sep24GetTransactionResponse

class Sep24Tests {
  companion object {
    private val jwtService =
      JwtService(SEP10_JWT_SECRET, SEP24_INTERACTIVE_URL_JWT_SECRET, SEP24_MORE_INFO_URL_JWT_SECRET)
    fun setup() {
      sep24Client = Sep24Client(toml.getString("TRANSFER_SERVER_SEP0024"), jwt)
    }
    fun `test Sep24 info endpoint`() {
      printRequest("Calling GET /info")
      val info = sep24Client.getInfo()
      JSONAssert.assertEquals(expectedSep24Info, gson.toJson(info), LENIENT)
    }

    fun `test Sep24 withdraw`() {
      printRequest("POST /transactions/withdraw/interactive")
      val withdrawRequest = gson.fromJson(withdrawRequest, HashMap::class.java)
      val response = sep24Client.withdraw(withdrawRequest as HashMap<String, String>)
      printResponse("POST /transactions/withdraw/interactive response:", response)
      savedWithdrawTxn = sep24Client.getTransaction(response.id, "USDC")
      printResponse(savedWithdrawTxn)
      JSONAssert.assertEquals(expectedSep24WithdrawResponse, json(savedWithdrawTxn), LENIENT)
      // check the returning Sep24InteractiveUrlJwt
      val params = UriComponentsBuilder.fromUriString(response.url).build().queryParams
      val cipher = params["token"]!![0]
      val jwt = jwtService.decode(cipher, Sep24InteractiveUrlJwt::class.java)
      val data = jwt.claims["data"] as Map<String, String>
      assertEquals(response.id, data["transaction_id"])
    }

    fun `test Sep24 deposit`() {
      printRequest("POST /transactions/withdraw/interactive")
      val depositRequest = gson.fromJson(depositRequest, HashMap::class.java)
      val response = sep24Client.deposit(depositRequest as HashMap<String, String>)
      printResponse("POST /transactions/deposit/interactive response:", response)
      savedDepositTxn = sep24Client.getTransaction(response.id, "USDC")
      printResponse(savedDepositTxn)
      JSONAssert.assertEquals(expectedSep24DepositResponse, json(savedDepositTxn), LENIENT)
      // check the returning Sep24InteractiveUrlJwt
      val params = UriComponentsBuilder.fromUriString(response.url).build().queryParams
      val cipher = params["token"]!![0]
      val jwt = jwtService.decode(cipher, Sep24InteractiveUrlJwt::class.java)
      val data = jwt.claims["data"] as Map<String, String>
      assertEquals(response.id, data["transaction_id"])
    }

    fun `test Sep24 GET transaction and check the JWT`() {
      val txn =
        sep24Client
          .getTransaction(
            savedDepositTxn.transaction.id,
            "stellar:USDC:GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP"
          )
          .transaction

      val params = UriComponentsBuilder.fromUriString(txn.moreInfoUrl).build().queryParams
      val cipher = params["token"]!![0]
      val jwt = jwtService.decode(cipher, Sep24MoreInfoUrlJwt::class.java)
      val data = jwt.claims["data"] as Map<String, String>
      assertEquals(txn.id, data["transaction_id"])
    }

    fun `test PlatformAPI GET transaction for deposit and withdrawal`() {
      val actualWithdrawTxn = platformApiClient.getTransaction(savedWithdrawTxn.transaction.id)
      assertEquals(actualWithdrawTxn.id, savedWithdrawTxn.transaction.id)
      println(expectedWithdrawTransactionResponse)
      println(json(actualWithdrawTxn))
      JSONAssert.assertEquals(expectedWithdrawTransactionResponse, json(actualWithdrawTxn), LENIENT)

      val actualDepositTxn = platformApiClient.getTransaction(savedDepositTxn.transaction.id)
      printResponse(actualDepositTxn)
      assertEquals(actualDepositTxn.id, savedDepositTxn.transaction.id)
      JSONAssert.assertEquals(expectedDepositTransactionResponse, json(actualDepositTxn), LENIENT)
    }

    fun `test patch, get and compare`() {
      val patch =
        gson.fromJson(patchWithdrawTransactionRequest, PatchTransactionsRequest::class.java)
      // create patch request and patch
      patch.records[0].transaction.id = savedWithdrawTxn.transaction.id
      patch.records[1].transaction.id = savedDepositTxn.transaction.id
      platformApiClient.patchTransaction(patch)

      // check if the patched transactions are as expected
      var afterPatchWithdraw = platformApiClient.getTransaction(savedWithdrawTxn.transaction.id)
      assertEquals(afterPatchWithdraw.id, savedWithdrawTxn.transaction.id)
      JSONAssert.assertEquals(expectedAfterPatchWithdraw, json(afterPatchWithdraw), LENIENT)

      var afterPatchDeposit = platformApiClient.getTransaction(savedDepositTxn.transaction.id)
      assertEquals(afterPatchDeposit.id, savedDepositTxn.transaction.id)
      JSONAssert.assertEquals(expectedAfterPatchDeposit, json(afterPatchDeposit), LENIENT)

      // Test patch idempotency
      afterPatchWithdraw = platformApiClient.getTransaction(savedWithdrawTxn.transaction.id)
      assertEquals(afterPatchWithdraw.id, savedWithdrawTxn.transaction.id)
      JSONAssert.assertEquals(expectedAfterPatchWithdraw, json(afterPatchWithdraw), LENIENT)

      afterPatchDeposit = platformApiClient.getTransaction(savedDepositTxn.transaction.id)
      assertEquals(afterPatchDeposit.id, savedDepositTxn.transaction.id)
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
  Sep24Tests.`test Sep24 GET transaction and check the JWT`()
  Sep24Tests.`test PlatformAPI GET transaction for deposit and withdrawal`()
  Sep24Tests.`test patch, get and compare`()
  Sep24Tests.`test GET transactions with bad ids`()
}

private const val withdrawRequest =
  """{
    "asset_code": "USDC",
    "asset_issuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
    "account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
    "lang": "en"
}"""

private const val depositRequest =
  """{
    "asset_code": "USDC",
    "asset_issuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
    "account": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
    "lang": "en"
}"""

private const val patchWithdrawTransactionRequest =
  """
{
  "records": [
    {
      "transaction": {
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
      }
    },
    {
      "transaction": {
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
    }
  ]
}    
"""

private const val expectedAfterPatchWithdraw =
  """
{
  "sep": "24",
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

private const val expectedAfterPatchDeposit =
  """
  {
    "sep": "24",
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

private const val expectedSep24Info =
  """
  {
    "deposit": {
      "JPYC": {
        "enabled": true,
        "min_amount": 1,
        "max_amount": 1000000
      },
      "USD": {
        "enabled": true,
        "min_amount": 0,
        "max_amount": 10000
      },
      "USDC": {
        "enabled": true,
        "min_amount": 1,
        "max_amount": 1000000
      }
    },
    "withdraw": {
      "JPYC": {
        "enabled": true,
        "min_amount": 1,
        "max_amount": 1000000
      },
      "USD": {
        "enabled": true,
        "min_amount": 0,
        "max_amount": 10000
      },
      "USDC": {
        "enabled": true,
        "min_amount": 1,
        "max_amount": 1000000
      }
    },
    "fee": {
      "enabled": true
    },
    "features": {
      "account_creation": false,
      "claimable_balances": false
    }
  }
"""

private const val expectedSep24WithdrawResponse =
  """
  {
    "transaction": {
      "kind": "withdrawal",
      "status": "incomplete",
      "refunded": false,
      "from": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4"
    }
  }
"""

private const val expectedSep24DepositResponse =
  """
  {
    "transaction": {
      "kind": "deposit",
      "status": "incomplete",
      "refunded": false,
      "to": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
    }
  }
"""

private const val expectedWithdrawTransactionResponse =
  """
  {
    "sep": "24",
    "kind": "withdrawal",
    "status": "incomplete"
  }
"""

private const val expectedDepositTransactionResponse =
  """
  {
    "sep": "24",
    "kind": "deposit",
    "status": "incomplete"
  }
"""
