package org.stellar.anchor.platform.test

import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.platform.CLIENT_WALLET_ACCOUNT
import org.stellar.anchor.platform.Sep6Client
import org.stellar.anchor.platform.gson
import org.stellar.anchor.util.Log
import org.stellar.anchor.util.Sep1Helper.TomlContent

class Sep6Tests(val toml: TomlContent, jwt: String) {
  private val sep6Client = Sep6Client(toml.getString("TRANSFER_SERVER"), jwt)

  private val expectedSep6Info =
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
              "cash": {},
              "bank_account": {}
            }
          }
        },
        "withdraw-exchange": {
          "USDC": {
            "enabled": true,
            "authentication_required": true,
            "types": {
              "cash": {},
              "bank_account": {}
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
            "status": "incomplete",
            "to": "GDJLBYYKMCXNVVNABOE66NYXQGIA5AC5D223Z2KF6ZEYK4UBCA7FKLTG"
        }
    }
  """
      .trimIndent()

  private fun `test Sep6 info endpoint`() {
    val info = sep6Client.getInfo()
    JSONAssert.assertEquals(expectedSep6Info, gson.toJson(info), JSONCompareMode.LENIENT)
  }

  private fun `test sep6 deposit`() {
    val request =
      mapOf(
        "asset_code" to "USDC",
        // TODO: this should be more obvious
        "account" to CLIENT_WALLET_ACCOUNT,
        "amount" to "0.01",
        "type" to "bank_account"
      )
    val response = sep6Client.deposit(request)
    Log.info("GET /deposit response: $response")
    assert(!response.id.isNullOrEmpty())
    assert(!response.how.isNullOrEmpty())

    val savedDepositTxn = sep6Client.getTransaction(mapOf("id" to response.id!!))
    JSONAssert.assertEquals(
      expectedSep6DepositResponse,
      gson.toJson(savedDepositTxn),
      JSONCompareMode.LENIENT
    )
  }

  fun testAll() {
    Log.info("Performing SEP6 tests")
    `test Sep6 info endpoint`()
    `test sep6 deposit`()
  }
}
