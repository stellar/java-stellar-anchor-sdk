package org.stellar.anchor.platform.data

class JdbcSep31TransactionTest {
  private val refundsJsonNoRefundPayment =
    """
            {
              "amount_refunded": "10",
              "amount_fee": "5",
              "refundPayments": null
            }
        """
      .trimIndent()

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
        """
      .trimIndent()
}
