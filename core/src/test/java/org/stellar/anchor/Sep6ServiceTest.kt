package org.stellar.anchor

import com.google.gson.Gson
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.sep.sep6.InfoResponse
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.sep6.Sep6Service
import org.stellar.anchor.util.GsonUtils

class Sep6ServiceTest {
  companion object {
    val gson: Gson = GsonUtils.getInstance()
  }

  private val assetService: AssetService = DefaultAssetService.fromJsonResource("test_assets.json")

  private lateinit var sep6Service: Sep6Service

  @BeforeEach
  fun setup() {
    sep6Service = Sep6Service(assetService)
  }

  private val infoJson =
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
        }
      }
    """
      .trimIndent()

  @Test
  fun `test INFO response`() {
    val infoResponse = sep6Service.info
    assertEquals(gson.fromJson(infoJson, InfoResponse::class.java), infoResponse)
  }
}
