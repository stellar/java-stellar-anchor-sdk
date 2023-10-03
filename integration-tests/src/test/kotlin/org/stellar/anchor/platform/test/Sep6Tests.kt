package org.stellar.anchor.platform.test

import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest
import org.stellar.anchor.platform.CLIENT_WALLET_ACCOUNT
import org.stellar.anchor.platform.Sep12Client
import org.stellar.anchor.platform.Sep6Client
import org.stellar.anchor.platform.gson
import org.stellar.anchor.util.Log
import org.stellar.anchor.util.Sep1Helper.TomlContent

class Sep6Tests(val toml: TomlContent, jwt: String) {
  private val sep6Client: Sep6Client
  private val sep12Client: Sep12Client

  init {
    sep6Client = Sep6Client(toml.getString("TRANSFER_SERVER"), jwt)
    sep12Client = Sep12Client(toml.getString("KYC_SERVER"), jwt)
  }

  private val expectedSep6Info =
    """
      {
          "deposit": {
              "USDC": {
                  "enabled": true,
                  "authentication_required": true,
                  "min_amount": 1,
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
                  "min_amount": 1,
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
                  "max_amount": 1000000,
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
                  "max_amount": 1000000,
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

  private val expectedSep6DepositResponse =
    """
    {
        "transaction": {
            "kind": "deposit",
            "to": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
        }
    }
  """
      .trimIndent()

  private val expectedSep6WithdrawResponse =
    """
      {
          "transaction": {
              "kind": "withdrawal",
              "status": "incomplete",
              "from": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG",
              "withdraw_memo_type": "hash"
          }
      }
    """
      .trimIndent()

  private fun `test Sep6 info endpoint`() {
    val info = sep6Client.getInfo()
    JSONAssert.assertEquals(expectedSep6Info, gson.toJson(info), JSONCompareMode.STRICT)
  }

  private fun `test sep6 deposit`() {
    val request =
      mapOf(
        "asset_code" to "USDC",
        "account" to CLIENT_WALLET_ACCOUNT,
        "amount" to "1",
        "type" to "SWIFT"
      )
    val response = sep6Client.deposit(request)
    Log.info("GET /deposit response: $response")
    assert(!response.id.isNullOrEmpty())

    val savedDepositTxn = sep6Client.getTransaction(mapOf("id" to response.id!!))
    JSONAssert.assertEquals(
      expectedSep6DepositResponse,
      gson.toJson(savedDepositTxn),
      JSONCompareMode.LENIENT
    )
  }

  private fun `test sep6 withdraw`() {
    val request = mapOf("asset_code" to "USDC", "type" to "bank_account", "amount" to "1")
    val response = sep6Client.withdraw(request)
    Log.info("GET /withdraw response: $response")
    assert(!response.id.isNullOrEmpty())

    val savedWithdrawTxn = sep6Client.getTransaction(mapOf("id" to response.id!!))
    JSONAssert.assertEquals(
      expectedSep6WithdrawResponse,
      gson.toJson(savedWithdrawTxn),
      JSONCompareMode.LENIENT
    )
  }

  private fun putCustomer() {
    val request =
      Sep12PutCustomerRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .emailAddress("john@email.com")
        .build()
    sep12Client.putCustomer(request)
  }

  fun testAll() {
    Log.info("Performing SEP6 tests")
    // Create a customer before running any tests
    putCustomer()
    `test Sep6 info endpoint`()
    `test sep6 deposit`()
    `test sep6 withdraw`()
  }
}
