package org.stellar.anchor.server.data

import com.google.gson.Gson
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.stellar.anchor.asset.AssetInfo
import org.stellar.anchor.util.GsonUtils

@DataJpaTest
class JdbcSep31TransactionTest {
  companion object {
    val gson: Gson = GsonUtils.getInstance()
  }

  @Autowired lateinit var txnRepo: JdbcSep31TransactionRepo

  @Test
  fun testInjectedComponentsAreNotNull() {
    assertNotNull(txnRepo)
  }

  @Test
  fun testBasic() {
    var txn = createTestTxn()
    val savedTxn = txnRepo.save(txn)
    txn = txnRepo.findById(savedTxn.id).get()
    assertNotNull(savedTxn)
    assertNotNull(txn)
    assertNotNull(txn.requiredInfoUpdates)
    assertNotNull(txn.requiredInfoUpdates.transaction)
    assertEquals(3, txn.requiredInfoUpdates.transaction.size)

    // Test requiredInfoUpdates modification
    txn.requiredInfoUpdates.transaction.remove("type")
    txnRepo.save(txn)
    txn = txnRepo.findById(txn.id).get()
    assertEquals(2, txn.requiredInfoUpdates.transaction.size)

    // Test delete
    txnRepo.delete(txn)
    assertTrue(txnRepo.findById(txn.id).isEmpty)
  }

  fun createTestTxn(): JdbcSep31Transaction {
    val txn = gson.fromJson(testTxnJson, JdbcSep31Transaction::class.java)
    txn.setRequiredInfoUpdates(
      gson.fromJson(testRequiredInfoUpdatesJson, AssetInfo.Sep31TxnFieldSpecs::class.java)
    )
    return txn
  }

  private val testRequiredInfoUpdatesJson =
    """
      {
        "transaction": {
          "receiver_routing_number": {
            "description": "routing number of the destination bank account"
          },
          "receiver_account_number": {
            "description": "bank account number of the destination"
          },
          "type": {
            "description": "type of deposit to make",
            "choices": [
              "SEPA",
              "SWIFT"
            ]
          }
        }
      }
    """

  private val testTxnJson =
    """
    {
      "id": "82fhs729f63dh0v4",
      "status": "pending_external",
      "status_eta": 3600,
      "stellar_transaction_id": "b9d0b2292c4e09e8eb22d036171491e87b8d2086bf8b265874c8d182cb9c9020",
      "external_transaction_id": "ABCDEFG1234567890",
      "stellar_account_id": "GBRPYHIL2CI3FNQ4BXLFMNDLFJUNPU2HY3ZMFSHONUCEOASW7QC7OX2H",
      "stellar_memo": "123456789",
      "stellar_memo_type": "id",
      "amount_in": "18.34",
      "amount_out": "18.24",
      "amount_fee": "0.1",
      "started_at": "2017-03-20T17:05:32Z"
    }
  """
}
