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
  val address = varchar("address", 255).nullable()
  val emailAddress = varchar("email_address", 255).nullable()
  val birthDate = varchar("birth_date", 255).nullable()
  val bankAccountNumber = varchar("bank_account_number", 255).nullable()
  val bankAccountType = varchar("bank_account_type", 255).nullable()
  val bankNumber = varchar("bank_number", 255).nullable()
  val bankBranchNumber = varchar("bank_branch_number", 255).nullable()
  val clabeNumber = varchar("clabe_number", 255).nullable()
  val idType = varchar("id_type", 255).nullable()
  val idCountryCode = varchar("id_country_code", 255).nullable()
  val idIssueDate = char("id_issue_date", 10).nullable()
  val idExpirationDate = char("id_expiration_date", 10).nullable()
  val idNumber = varchar("id_number", 255).nullable()
}
