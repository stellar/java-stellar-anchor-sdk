@file:Suppress("UNCHECKED_CAST")

package org.stellar.anchor.platform.test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.LENIENT
import org.springframework.web.util.UriComponentsBuilder
import org.stellar.anchor.api.exception.SepException
import org.stellar.anchor.api.platform.PatchTransactionsRequest
import org.stellar.anchor.api.sep.sep24.Sep24GetTransactionResponse
import org.stellar.anchor.apiclient.PlatformApiClient
import org.stellar.anchor.auth.AuthHelper
import org.stellar.anchor.auth.JwtService
import org.stellar.anchor.auth.Sep24InteractiveUrlJwt
import org.stellar.anchor.auth.Sep24MoreInfoUrlJwt
import org.stellar.anchor.platform.*
import org.stellar.anchor.util.Sep1Helper.*

lateinit var savedWithdrawTxn: Sep24GetTransactionResponse
lateinit var savedDepositTxn: Sep24GetTransactionResponse

class Sep24Tests(val config: TestConfig, val toml: TomlContent, jwt: String) {
  private val jwtService: JwtService =
    JwtService(
      config.env["secret.sep10.jwt_secret"]!!,
      config.env["secret.sep24.interactive_url.jwt_secret"]!!,
      config.env["secret.sep24.more_info_url.jwt_secret"]!!,
      config.env["secret.callback_api.auth_secret"]!!,
      config.env["secret.platform_api.auth_secret"]!!
    )

  private val platformApiClient =
    PlatformApiClient(AuthHelper.forNone(), config.env["platform.server.url"]!!)
  private val sep24Client = Sep24Client(toml.getString("TRANSFER_SERVER_SEP0024"), jwt)

  private fun `test Sep24 info endpoint`() {
    printRequest("Calling GET /info")
    val info = sep24Client.getInfo()
    JSONAssert.assertEquals(expectedSep24Info, gson.toJson(info), LENIENT)
  }

  private fun `test Sep24 withdraw`() {
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
    assertEquals(response.id, jwt.jti)
  }

  private fun `test Sep24 deposit`() {
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
    assertEquals(response.id, jwt.jti)
  }

  private fun `test Sep24 GET transaction and check the JWT`() {
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
    assertEquals(txn.id, jwt.jti)
  }

  private fun `test PlatformAPI GET transaction for deposit and withdrawal`() {
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

  private fun `test patch, get and compare`() {
    val patch = gson.fromJson(patchWithdrawTransactionRequest, PatchTransactionsRequest::class.java)
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

  private fun `test GET transactions with bad ids`() {
    val badTxnIds = listOf("null", "bad id", "123", null)
    for (txnId in badTxnIds) {
      assertThrows<SepException> { platformApiClient.getTransaction(txnId) }
    }
  }

  fun testAll() {
    println("Performing SEP24 tests...")
    `test Sep24 info endpoint`()
    `test Sep24 withdraw`()
    `test Sep24 deposit`()
    `test Sep24 GET transaction and check the JWT`()
    `test PlatformAPI GET transaction for deposit and withdrawal`()
    `test patch, get and compare`()
    `test GET transactions with bad ids`()
  }
}

private const val withdrawRequest =
  """{
    "amount": "10",
    "asset_code": "USDC",
    "asset_issuer": "GDQOE23CFSUMSVQK4Y5JHPPYK73VYCNHZHA7ENKCV37P6SUEO6XQBKPP",
    "account": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
    "lang": "en"
}"""

private const val depositRequest =
  """{
    "amount": "10",
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
        "enabled": true
      },
      "USD": {
        "enabled": true
      },
      "USDC": {
        "enabled": true
      }
    },
    "withdraw": {
      "JPYC": {
        "enabled": true
      },
      "USD": {
        "enabled": true
      },
      "USDC": {
        "enabled": true
      }
    },
    "fee": {
      "enabled": false
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
