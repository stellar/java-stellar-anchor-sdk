package com.example.data

import io.ktor.server.application.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
  val id: String,
  val status: String,
  val kind: String,
  val message: String? = null,
  @SerialName("amount_in") val amountIn: Amount? = null,
  @SerialName("amount_out") val amountOut: Amount? = null,
  @SerialName("amount_fee") val amountFee: Amount? = null,
  @SerialName("amount_expected") val amountExpected: Amount? = null,
  @SerialName("memo") val memo: String? = null,
  @SerialName("memo_type") val memoType: String? = null,
  @SerialName("source_account") var sourceAccount: String? = null,
  @SerialName("destination_account") var destinationAccount: String? = null,
  @SerialName("stellar_transactions") val stellarTransactions: List<StellarTransaction>? = null
)

@Serializable data class PatchTransactionsRequest(val records: List<PatchTransactionRecord>)

@Serializable data class PatchTransactionRecord(val transaction: PatchTransactionTransaction)

@Serializable
data class PatchTransactionTransaction(
  val id: String,
  val status: String,
  val message: String? = null,
  @SerialName("kyc_verified") val kycVerified: String? = null,
  @SerialName("amount_in") val amountIn: Amount? = null,
  @SerialName("amount_out") val amountOut: Amount? = null,
  @SerialName("amount_fee") val amountFee: Amount? = null,
  @SerialName("stellar_transaction_id") val stellarTransactionId: String? = null,
  val memo: String? = null,
  @SerialName("memo_type") val memoType: String? = null,
)

@Serializable data class Amount(val amount: String? = null, val asset: String? = null)

class JwtToken(
  val transactionId: String,
  var expiration: Long, // Expiration Time
)

@Serializable
data class DepositRequest(
  val amount: String,
  val name: String,
  val surname: String,
  val email: String
)

@Serializable
data class WithdrawalRequest(
  val amount: String,
  val name: String,
  val surname: String,
  val email: String,
  val bank: String,
  val account: String
)

@Serializable
data class StellarTransaction(
  val id: String,
  val memo: String? = null,
  @SerialName("memo_type") val memoType: String? = null,
  val payments: List<StellarPayment>
)

@Serializable data class StellarPayment(val id: String, val amount: Amount)
