@file:Suppress("UNCHECKED_CAST")

package org.stellar.anchor.platform

import org.junit.jupiter.api.Assertions.assertEquals
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.JSONCompareMode.LENIENT
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

    fun `test PlatformAPI GET transaction`() {
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

    fun `test Sep24 deposit`() {
      printRequest("POST /transactions/withdraw/interactive")
      val depositRequest = gson.fromJson(depositRequest, HashMap::class.java)
      val txn = sep24Client.deposit(depositRequest as HashMap<String, String>)
      printResponse("POST /transactions/deposit/interactive response:", txn)
      savedDepositTransaction = sep24Client.getTransaction(txn.id, "USDC")
      printResponse(savedDepositTransaction)
      JSONAssert.assertEquals(expectedSep24DepositResponse, json(savedDepositTransaction), LENIENT)
    }
  }
}

fun sep24TestAll() {
  Sep24Tests.setup()

  println("Performing SEP24 tests...")
  Sep24Tests.`test Sep24 info endpoint`()
  Sep24Tests.`test Sep24 withdraw`()
  Sep24Tests.`test Sep24 deposit`()
  Sep24Tests.`test PlatformAPI GET transaction`()
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
      "amount_in": "0",
      "amount_out": "0",
      "amount_fee": "0",
      "stellar_transaction_id": "",
      "refunded": false,
      "from": "GAIUIZPHLIHQEMNJGSZKCEUWHAZVGUZDBDMO2JXNAJZZZVNSVHQCEWJ4",
      "to": ""
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
      "amount_in": "0",
      "amount_out": "0",
      "amount_fee": "0",
      "stellar_transaction_id": "",
      "refunded": false,
      "from": "",
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
