package org.stellar.anchor

import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.sep.sep6.InfoResponse
import org.stellar.anchor.asset.AssetService
import org.stellar.anchor.asset.DefaultAssetService
import org.stellar.anchor.config.Sep6Config
import org.stellar.anchor.sep6.Sep6Service
import org.stellar.anchor.util.GsonUtils

class Sep6ServiceTest {
  companion object {
    val gson: Gson = GsonUtils.getInstance()
  }

  private val assetService: AssetService = DefaultAssetService.fromJsonResource("test_assets.json")

  @MockK(relaxed = true) lateinit var sep6Config: Sep6Config

  private lateinit var sep6Service: Sep6Service

  @BeforeEach
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { sep6Config.features.isAccountCreation } returns false
    every { sep6Config.features.isClaimableBalances } returns false
    sep6Service = Sep6Service(sep6Config, assetService)
  }

  @AfterEach
  fun teardown() {
    clearAllMocks()
    unmockkAll()
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
  fun `test INFO response`() {
    val infoResponse = sep6Service.info
    assertEquals(gson.fromJson(infoJson, InfoResponse::class.java), infoResponse)
  }
}
