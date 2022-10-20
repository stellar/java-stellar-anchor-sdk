package org.stellar.anchor.platform.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class JdbcSep31TransactionTest {
  private val refundsJsonNoRefundPayment =
    """
            {
              "amount_refunded": "10",
              "amount_fee": "5",
              "refundPayments": null
            }
        """.trimIndent()

  private val refundsJsonWithRefundPayment =
    """
            {
              "amount_refunded": "10",
              "amount_fee": "5",
              "payments": [
                {
                  "id": "1",
                  "amount": "5",
                  "fee": "1"
                },
                {
                  "id": "2",
                  "amount": "5",
                  "fee": "4"
                }
              ]
            }
        """.trimIndent()

  @Test
  fun `test JdbcSep31Transaction refunds Json conversion`() {
    val txn = JdbcSep31Transaction()
    txn.refundsJson = refundsJsonNoRefundPayment
    // strict is set to false because refundPayments is omitted in txn.refundsJson when it is set to
    // null.
    JSONAssert.assertEquals(txn.refundsJson, refundsJsonNoRefundPayment, false)
    assertEquals("10", txn.refunds.amountRefunded)
    assertEquals("5", txn.refunds.amountFee)
    assertNull(txn.refunds.refundPayments)

    txn.refundsJson = refundsJsonWithRefundPayment
    JSONAssert.assertEquals(txn.refundsJson, refundsJsonWithRefundPayment, true)
    assertEquals("10", txn.refunds.amountRefunded)
    assertEquals("5", txn.refunds.amountFee)
    assertEquals(2, txn.refunds.refundPayments.size)
    assertEquals("1", txn.refunds.refundPayments[0].id)
    assertEquals("5", txn.refunds.refundPayments[0].amount)
    assertEquals("1", txn.refunds.refundPayments[0].fee)
    assertEquals("2", txn.refunds.refundPayments[1].id)
    assertEquals("5", txn.refunds.refundPayments[1].amount)
    assertEquals("4", txn.refunds.refundPayments[1].fee)
  }
}
