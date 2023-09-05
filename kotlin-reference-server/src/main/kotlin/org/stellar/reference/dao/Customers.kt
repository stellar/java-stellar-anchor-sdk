package org.stellar.reference.dao

import org.jetbrains.exposed.sql.Table

/** Exposed table for the Customer model. */
object Customers : Table() {
  val id = varchar("id", 255).uniqueIndex()
  val stellarAccount = varchar("stellar_account", 255).index().nullable()
  val memo = varchar("memo", 255).nullable()
  val memoType = varchar("memo_type", 255).nullable()
  val firstName = varchar("first_name", 255).nullable()
  val lastName = varchar("last_name", 255).nullable()
  val emailAddress = varchar("email_address", 255).nullable()
  val bankAccountNumber = varchar("bank_account_number", 255).nullable()
  val bankAccountType = varchar("bank_account_type", 255).nullable()
  val bankRoutingNumber = varchar("bank_routing_number", 255).nullable()
  val clabeNumber = varchar("clabe_number", 255).nullable()
}
