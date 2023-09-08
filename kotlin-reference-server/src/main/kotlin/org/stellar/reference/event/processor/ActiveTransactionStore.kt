package org.stellar.reference.event.processor

interface ActiveTransactionStore {
  fun addTransaction(customerId: String, transactionId: String)
  fun removeTransaction(customerId: String, transactionId: String)
  fun getTransactions(customerId: String): List<String>
}

class InMemoryTransactionStore : ActiveTransactionStore {
  private val transactions = mutableMapOf<String, MutableSet<String>>()

  override fun addTransaction(customerId: String, transactionId: String) {
    transactions.getOrPut(customerId) { mutableSetOf() }.add(transactionId)
  }

  override fun removeTransaction(customerId: String, transactionId: String) {
    transactions[customerId]?.remove(transactionId)
  }

  override fun getTransactions(customerId: String): List<String> {
    return transactions[customerId]?.toList() ?: emptyList()
  }
}
