package org.stellar.anchor.platform.subtest

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.stellar.anchor.platform.Sep6Client
import org.stellar.anchor.platform.TestConfig
import org.stellar.anchor.platform.gson

class Sep6Tests : SepTests(TestConfig(testProfileName = "default")) {
  private val sep6Client = Sep6Client(toml.getString("TRANSFER_SERVER"))

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

  @Test
  fun `test Sep6 info endpoint`() {
    val info = sep6Client.getInfo()
    JSONAssert.assertEquals(expectedSep6Info, gson.toJson(info), JSONCompareMode.LENIENT)
  }
}
