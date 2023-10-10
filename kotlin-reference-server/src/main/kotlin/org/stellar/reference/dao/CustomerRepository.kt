package org.stellar.reference.dao

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.transactions.transaction
import org.stellar.reference.model.Customer

interface CustomerRepository {
  fun get(id: String): Customer?

  fun get(stellarAccount: String, memo: String?, memoType: String?): Customer?

  fun create(customer: Customer): String?

  fun update(customer: Customer)

  fun delete(id: String)
}

class JdbcCustomerRepository(private val db: Database) : CustomerRepository {
  init {
    transaction(db) { SchemaUtils.create(Customers) }
  }

  override fun get(id: String): Customer? =
    transaction(db) {
        Customers.select { Customers.id.eq(id) }
          .mapNotNull {
            Customer(
              id = id,
              stellarAccount = it[Customers.stellarAccount],
              memo = it[Customers.memo],
              memoType = it[Customers.memoType],
              firstName = it[Customers.firstName],
              address = it[Customers.address],
              lastName = it[Customers.lastName],
              emailAddress = it[Customers.emailAddress],
              bankAccountNumber = it[Customers.bankAccountNumber],
              bankAccountType = it[Customers.bankAccountType],
              bankNumber = it[Customers.bankNumber],
              bankBranchNumber = it[Customers.bankBranchNumber],
              clabeNumber = it[Customers.clabeNumber],
              idType = it[Customers.idType],
              idCountryCode = it[Customers.idCountryCode],
              idIssueDate = it[Customers.idIssueDate],
              idExpirationDate = it[Customers.idExpirationDate],
              idNumber = it[Customers.idNumber]
            )
          }
      }
      .singleOrNull()

  override fun get(stellarAccount: String, memo: String?, memoType: String?): Customer? {
    val query =
      when {
        memo == null && memoType == null -> {
          Customers.stellarAccount.eq(stellarAccount) and
            Customers.memo.isNull() and
            Customers.memoType.isNull()
        }
        else -> {
          Customers.stellarAccount.eq(stellarAccount) and
            Customers.memo.eq(memo!!) and
            Customers.memoType.eq(memoType!!)
        }
      }
    return transaction(db) {
        Customers.select { query }
          .mapNotNull {
            Customer(
              id = it[Customers.id],
              stellarAccount = it[Customers.stellarAccount],
              memo = it[Customers.memo],
              memoType = it[Customers.memoType],
              firstName = it[Customers.firstName],
              lastName = it[Customers.lastName],
              address = it[Customers.address],
              emailAddress = it[Customers.emailAddress],
              bankAccountNumber = it[Customers.bankAccountNumber],
              bankAccountType = it[Customers.bankAccountType],
              bankNumber = it[Customers.bankNumber],
              bankBranchNumber = it[Customers.bankBranchNumber],
              clabeNumber = it[Customers.clabeNumber],
              idType = it[Customers.idType],
              idCountryCode = it[Customers.idCountryCode],
              idIssueDate = it[Customers.idIssueDate],
              idExpirationDate = it[Customers.idExpirationDate],
              idNumber = it[Customers.idNumber]
            )
          }
      }
      .singleOrNull()
  }

  override fun create(customer: Customer): String? =
    transaction(db) {
        Customers.insert {
          it[id] = customer.id!!
          it[stellarAccount] = customer.stellarAccount
          it[memo] = customer.memo
          it[memoType] = customer.memoType
          it[firstName] = customer.firstName
          it[lastName] = customer.lastName
          it[address] = customer.address
          it[emailAddress] = customer.emailAddress
          it[bankAccountNumber] = customer.bankAccountNumber
          it[bankAccountType] = customer.bankAccountType
          it[bankNumber] = customer.bankNumber
          it[bankAccountNumber] = customer.bankAccountNumber
          it[clabeNumber] = customer.clabeNumber
          it[idType] = customer.idType
          it[idCountryCode] = customer.idCountryCode
          it[idIssueDate] = customer.idIssueDate
          it[idExpirationDate] = customer.idExpirationDate
          it[idNumber] = customer.idNumber
        }
      }
      .resultedValues
      ?.firstOrNull()
      ?.get(Customers.id)

  override fun update(customer: Customer): Unit =
    transaction(db) {
      Customers.update({ Customers.id.eq(customer.id!!) }) {
        it[stellarAccount] = customer.stellarAccount
        it[memo] = customer.memo
        it[memoType] = customer.memoType
        it[firstName] = customer.firstName
        it[lastName] = customer.lastName
        it[address] = customer.address
        it[emailAddress] = customer.emailAddress
        it[bankAccountNumber] = customer.bankAccountNumber
        it[bankAccountType] = customer.bankAccountType
        it[bankNumber] = customer.bankNumber
        it[bankBranchNumber] = customer.bankBranchNumber
        it[clabeNumber] = customer.clabeNumber
        it[idType] = customer.idType
        it[idCountryCode] = customer.idCountryCode
        it[idIssueDate] = customer.idIssueDate
        it[idExpirationDate] = customer.idExpirationDate
        it[idNumber] = customer.idNumber
      }
    }

  override fun delete(id: String): Unit =
    transaction(db) { Customers.deleteWhere { Customers.id.eq(id) } }
}
